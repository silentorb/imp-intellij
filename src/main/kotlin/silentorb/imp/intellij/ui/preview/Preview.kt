package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.TimerUtil
import silentorb.imp.core.*
import silentorb.imp.intellij.common.getExecutionSteps
import silentorb.imp.intellij.common.getOutputNode
import silentorb.imp.intellij.services.*
import silentorb.imp.intellij.ui.misc.getActiveVirtualFile
import silentorb.imp.intellij.ui.misc.getDocumentFromPath
import silentorb.imp.intellij.ui.misc.getDungeonWithoutErrors
import silentorb.imp.intellij.ui.misc.replacePanelContents
import silentorb.imp.parsing.general.englishText
import silentorb.mythic.spatial.Vector2i
import java.awt.BorderLayout
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class PreviewContainer(val project: Project, contentManager: ContentManager) : JPanel(), Disposable {
  var display: PreviewDisplay? = null
  var document: Document? = null
  var state: PreviewState? = null
  var previewLockFile: String? = null

  init {
    layout = BorderLayout()

    DumbService.getInstance(project).runWhenSmart {
      Disposer.register(contentManager, this)

      val timer = TimerUtil.createNamedTimer("ActiveDocumentWatcher", 33) { onTick() }

      Disposer.register(this, Disposable {
        timer.stop()
      })

      timer.start()
      Disposer.register(contentManager, this)
    }
  }

  fun onTick() {
    val file = getActiveVirtualFile(project)
    val nextDocument = if (file != null)
      FileDocumentManager.getInstance().getDocument(file)!!
    else
      null

    val node = if (nextDocument != null) getDocumentMetadataService().getPreviewNode(nextDocument) else null
    val localPreviewLockFile = getPreviewFileLock()
    previewLockFile = localPreviewLockFile
    val dungeonDocument = if (localPreviewLockFile != null)
      getDocumentFromPath(localPreviewLockFile)!!
    else
      nextDocument ?: document

    val newDungeon = if (dungeonDocument != null)
      getDungeonWithoutErrors(project, dungeonDocument)
    else
      null

    if (newDungeon != state?.dungeon || node != state?.node || nextDocument != document) {
      if (document != nextDocument && nextDocument != null && document != null) {
        val k = 0
      }
      println("Active document contents changed")
      update(this, nextDocument, newDungeon, listOf(), node)
      document = nextDocument
    }
  }

  override fun dispose() {
  }
}

fun messagePanel(message: String): JComponent {
  return JBScrollPane(JBLabel(message))
}

fun newPreview(document: Document?, type: TypeHash, dimensions: Vector2i): PreviewDisplay? {
  val display = previewTypes().get(type)
  return if (display != null)
    display(NewPreviewProps(dimensions, document))
  else
    null
}

private val sourceLock = ReentrantLock()

fun updatePreviewState(
    document: Document?,
    type: TypeHash,
    dungeon: Dungeon,
    timestamp: Long,
    container: PreviewContainer,
    node: PathKey?
): PreviewState {
  val output = getOutputNode(document, node, dungeon)
  val executionUnit = if (output != null && document != null) {
    getExecutionSteps(document, output, dungeon)
  } else
    null

  sourceLock.lock()
  val state = PreviewState(
      document = document,
      type = type,
      dungeon = dungeon,
      node = node,
      executionUnit = executionUnit,
      timestamp = timestamp
  )
  container.state = state
  sourceLock.unlock()
  return state
}

fun updatePreview(
    document: Document?,
    preview: PreviewContainer,
    dungeon: Dungeon,
    type: TypeHash,
    timestamp: Long,
    node: PathKey?
) {
  if (type != preview.state?.type || document != preview.document) {
    val newDisplay = newPreview(document, type, Vector2i(preview.width))
    if (newDisplay != null) {
      println("New preview display")
      replacePanelContents(preview, newDisplay.content)
      val oldComponent = preview.display?.content
      if (oldComponent is Disposable) {
        Disposer.dispose(oldComponent)
      }
      preview.display = newDisplay
      val component = newDisplay.content
      if (component is Disposable) {
        Disposer.register(preview, component)
      }
    } else {
      val typeName = dungeon.graph.typings.typeNames[type] ?: "???"
      replacePanelContents(preview, messagePanel("No preview for type $typeName"))
    }
  }
  val display = preview.display
  if (display != null) {
    println("Updating preview display")
    val state = updatePreviewState(document, type, dungeon, timestamp, preview, node)
    var i = 0
    fun tryUpdate() {
      if (i++ < 100) {
        SwingUtilities.invokeLater {
          if (display.content.width != 0) {
            println("Updating preview at try $i")
            display.update(state)
          } else {
            tryUpdate()
          }
        }
      }
    }
    tryUpdate()
  }
}

private val timestampLock = ReentrantLock()

private var currentTimestamp = 0L

fun isPreviewOutdated(timestamp: Long) =
    timestamp < currentTimestamp

fun trySetPreviewTimestamp(timestamp: Long): Boolean {
  timestampLock.lock()
  return if (timestamp > currentTimestamp) {
    currentTimestamp = timestamp
    timestampLock.unlock()
    true
  } else {
    timestampLock.unlock()
    false
  }
}

fun updatePreview(document: Document?, dungeon: Dungeon, preview: PreviewContainer, timestamp: Long, node: PathKey?) {
  if (!trySetPreviewTimestamp(timestamp))
    return

  val output = getOutputNode(document, node, dungeon)
  val type = dungeon.graph.returnTypes[output]
  if (type != null) {
    updatePreview(document, preview, dungeon, type, timestamp, node)
  } else {
    sourceLock.lock()
    preview.state = PreviewState(
        document = document,
        type = unknownType.hash,
        dungeon = dungeon,
        node = node,
        executionUnit = null,
        timestamp = timestamp
    )
    sourceLock.unlock()
    replacePanelContents(preview, messagePanel("No preview for this type"))
  }
}

fun update(container: PreviewContainer, document: Document?, dungeon: Dungeon?, errors: ImpErrors, node: PathKey?) {
  if (dungeon == null) {
    container.state = null
    container.display = null
    container.removeAll()
    container.revalidate()
    container.repaint()
  } else if (errors.any()) {
    container.state = null
    container.display = null
    val errorPanel = messagePanel(formatError(::englishText, errors.first()))
    container.removeAll()
    container.add(errorPanel, BorderLayout.CENTER)
    container.revalidate()
    container.repaint()
  } else {
    println("Updating preview")
    updatePreview(document, dungeon, container, System.currentTimeMillis(), node)
  }
}
