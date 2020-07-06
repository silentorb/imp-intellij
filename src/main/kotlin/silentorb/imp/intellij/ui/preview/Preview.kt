package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.TimerUtil
import silentorb.imp.campaign.findContainingModule
import silentorb.imp.campaign.getModulesExecutionArtifacts
import silentorb.imp.core.*
import silentorb.imp.execution.ExecutionStep
import silentorb.imp.execution.prepareExecutionSteps
import silentorb.imp.intellij.services.*
import silentorb.imp.intellij.ui.misc.*
import silentorb.imp.parsing.general.englishText
import silentorb.mythic.spatial.Vector2i
import java.awt.BorderLayout
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

data class PreviewDisplay(
    val content: JComponent,
    val toolbar: ActionToolbar? = null,
    val update: (PreviewState) -> Unit
)

data class PreviewState(
    val document: Document?,
    val dungeon: Dungeon,
    val node: PathKey?,
    val steps: List<ExecutionStep>,
    val type: TypeHash,
    val timestamp: Long
)

fun getDocumentPath(document: Document): Path =
    Paths.get(FileDocumentManager.getInstance().getFile(document)!!.path)

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
      document = nextDocument
      println("Active document contents changed")
      update(this, nextDocument, newDungeon, listOf(), node)
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

fun getOutputNode(document: Document?, node: PathKey?, dungeon: Dungeon): PathKey? {
  return node ?: if (document != null)
    getGraphOutputNode(dungeon, getDocumentFile(document)?.canonicalPath!!)
  else
    null
}

fun updatePreviewState(
    document: Document?,
    type: TypeHash,
    dungeon: Dungeon,
    timestamp: Long,
    container: PreviewContainer,
    node: PathKey?
): PreviewState {
  val output = getOutputNode(document, node, dungeon)
  val steps = if (output != null && document != null) {
    val filePath = getDocumentPath(document)
    val workspaceResponse = getWorkspaceArtifact(filePath)
    val moduleDirectory = findContainingModule(filePath)
    try {
      if (workspaceResponse != null && moduleDirectory != null && workspaceResponse.value.modules.containsKey(
              moduleDirectory.fileName.toString()
          )
      ) {
        val (context, functions) = getModulesExecutionArtifacts(
            initialFunctions(),
            initialContext(),
            workspaceResponse.value.modules
        )
        prepareExecutionSteps(context, functions, setOf(output))
      } else
        prepareExecutionSteps(listOf(dungeon.graph), initialFunctions(), setOf(output))
    } catch (error: Error) {
      listOf<ExecutionStep>()
    }
  } else
    listOf()

  sourceLock.lock()
  val state = PreviewState(
      document = document,
      type = type,
      dungeon = dungeon,
      node = node,
      steps = steps,
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
  if (type != preview.state?.type) {
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
        steps = listOf(),
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
