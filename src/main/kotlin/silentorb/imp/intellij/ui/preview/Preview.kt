package silentorb.imp.intellij.ui.preview

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.ContentManager
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.TimerUtil
import silentorb.imp.campaign.findContainingModule
import silentorb.imp.campaign.getModulesExecutionArtifacts
import silentorb.imp.core.*
import silentorb.imp.execution.arrangeGraphSequence
import silentorb.imp.intellij.messaging.NodePreviewNotifier
import silentorb.imp.intellij.messaging.nodePreviewTopic
import silentorb.imp.intellij.services.getDocumentMetadataService
import silentorb.imp.parsing.general.ParsingErrors
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.imp.execution.ExecutionStep
import silentorb.imp.execution.prepareExecutionSteps
import silentorb.imp.intellij.services.getPreviewFileLock
import silentorb.imp.intellij.services.getWorkspaceArtifact
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.ui.misc.*
import silentorb.mythic.spatial.Vector2i
import java.awt.GridLayout
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

data class PreviewDisplay(
    val content: JComponent,
    val toolbar: ActionToolbar? = null,
    val update: (PreviewState) -> Unit
)

data class PreviewState(
    val document: Document?,
    val graph: Graph,
    val node: PathKey?,
    val steps: List<ExecutionStep>,
    val type: TypeHash,
    val timestamp: Long
)

fun getDocumentPath(document: Document): Path =
    Paths.get(FileDocumentManager.getInstance().getFile(document)!!.path)

class PreviewContainer(project: Project, contentManager: ContentManager) : JPanel(), Disposable {
  var display: PreviewDisplay? = null
  var previousDocument: Document? = null
  val connection: MessageBusConnection
  var lastDungeon: Dungeon? = null
  var lastErrors: ParsingErrors = listOf()
  var state: PreviewState? = null
  var nextUpdatedDocument: Document? = null
  var nextUpdatedTime: Long? = null
  var lastPreviewLockFile: String? = null

  val documentListener: DocumentListener = object : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      nextUpdatedDocument = event.document
      nextUpdatedTime = System.currentTimeMillis()
    }
  }

  val activeDocumentWatcher = ActiveDocumentWatcher(project, watchParsed(project) { dungeon, document, errors ->
    if (document != previousDocument) {
      if (previousDocument != null) {
        previousDocument!!.removeDocumentListener(documentListener)
      }
      if (document != null) {
        document.addDocumentListener(documentListener)
      }
      previousDocument = document
      println("Active document changed")
      if (getPreviewFileLock() == null) {
        update(dungeon, errors)
      }
    }
//    println("file change: ${file?.name ?: "none"}")
  })

  init {
    val bus = ApplicationManager.getApplication().getMessageBus()
    connection = bus.connect()
    connection.subscribe(nodePreviewTopic, object : NodePreviewNotifier {
      override fun handle(document: Document, node: PathKey?) {
        if (document == previousDocument) {
          update(lastDungeon, lastErrors)
        }
      }
    })

    DumbService.getInstance(project).runWhenSmart {
      activeDocumentWatcher.start(contentManager)
      layout = GridLayout(0, 1)
      Disposer.register(contentManager, this)

      val timer = TimerUtil.createNamedTimer("ActiveDocumentWatcher", 33) {
        val document = nextUpdatedDocument
        val updatedTime = nextUpdatedTime
        val previewLockFile = getPreviewFileLock()
        val fileChanged = updatedTime != null && System.currentTimeMillis() > updatedTime + 5
        if ((document != null && fileChanged) || previewLockFile != lastPreviewLockFile) {
          lastPreviewLockFile = previewLockFile
          nextUpdatedDocument = null
          nextUpdatedTime = null
          val dungeonDocument = if (previewLockFile != null)
            getDocumentFromPath(previewLockFile)!!
          else
            document ?: previousDocument

          if (dungeonDocument != null) {
            val response = getDungeonAndErrors(project, dungeonDocument)
            if (response != null) {
              val (dungeon, errors) = response
              println("Active document contents changed ${dungeon.graph.hashCode()}")
              update(dungeon, errors)
            }
          }
        }
      }

      Disposer.register(this, Disposable {
        timer.stop()
      })

      timer.start()
      Disposer.register(contentManager, this)
    }
  }

  fun update(dungeon: Dungeon?, errors: ParsingErrors) {
    lastDungeon = dungeon
    lastErrors = errors
    val documentMetadata = getDocumentMetadataService()
    val document = previousDocument
    val node = if (document != null)
      documentMetadata.getPreviewNode(document)
    else
      null

    update(this, document, dungeon, errors, node)
  }

  override fun dispose() {
    val document = previousDocument
    if (document != null) {
      try {
        document.removeDocumentListener(documentListener)
      } catch (error: Error) {
      }
    }
    connection.disconnect()
  }
}

fun messagePanel(message: String): JPanel {
  val panel = JPanel()
  panel.add(JLabel(message))
  return panel
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
    graph: Graph,
    timestamp: Long,
    container: PreviewContainer,
    node: PathKey?
): PreviewState {
  val output = node ?: getGraphOutputNode(graph)
  val steps = if (output != null && document != null) {
    val filePath = getDocumentPath(document)
    val workspaceResponse = getWorkspaceArtifact(filePath)
    val moduleDirectory = findContainingModule(filePath)
    if (workspaceResponse != null && moduleDirectory != null && workspaceResponse.value.modules.containsKey(moduleDirectory.fileName.toString())) {
      val (context, functions) = getModulesExecutionArtifacts(initialFunctions(), listOf(), workspaceResponse.value.modules)
      prepareExecutionSteps(context, functions, setOf(output))
    } else
      prepareExecutionSteps(listOf(graph), initialFunctions(), setOf(output))
  } else
    listOf()

  sourceLock.lock()
  val state = PreviewState(
      document = document,
      type = type,
      graph = graph,
      node = node,
      steps = steps,
      timestamp = timestamp
  )
  container.state = state
  sourceLock.unlock()
  return state
}

fun updatePreview(document: Document?, preview: PreviewContainer, graph: Graph, type: TypeHash, timestamp: Long, node: PathKey?) {
  if (type != preview.state?.type) {
    val newDisplay = newPreview(document, type, Vector2i(preview.width))
    if (newDisplay != null) {
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
      val typeName = graph.typings.typeNames[type] ?: "???"
      replacePanelContents(preview, messagePanel("No preview for type $typeName"))
    }
  }
  val state = updatePreviewState(document, type, graph, timestamp, preview, node)
  // The layout of new preview children isn't fully initialized until after this UI tick
  SwingUtilities.invokeLater {
//    SwingUtilities.invokeLater {
    val display = preview.display
    if (display != null) {
      if (display.content.width != 0) {
        display.update(state)
      }
//      }
    }
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

fun updatePreview(document: Document?, graph: Graph, preview: PreviewContainer, timestamp: Long, node: PathKey?) {
  if (!trySetPreviewTimestamp(timestamp))
    return

  val output = node ?: getGraphOutputNode(graph)
  val type = graph.returnTypes[output]
  if (type != null) {
    updatePreview(document, preview, graph, type, timestamp, node)
  } else {
    sourceLock.lock()
    preview.state = PreviewState(
        document = document,
        type = unknownType.hash,
        graph = graph,
        node = node,
        steps = listOf(),
        timestamp = timestamp
    )
    sourceLock.unlock()
    replacePanelContents(preview, messagePanel("No preview for this type"))
  }
}

fun update(container: PreviewContainer, document: Document?, dungeon: Dungeon?, errors: ParsingErrors, node: PathKey?) {
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
    container.add(errorPanel)
    container.revalidate()
    container.repaint()
  } else {
    updatePreview(document, dungeon.graph, container, System.currentTimeMillis(), node)
  }
}
