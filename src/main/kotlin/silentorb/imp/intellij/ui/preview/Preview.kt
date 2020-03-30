package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.ContentManager
import com.intellij.util.messages.MessageBusConnection
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.core.PathKey
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.arrangeGraphSequence
import silentorb.imp.intellij.messaging.NodePreviewNotifier
import silentorb.imp.intellij.messaging.nodePreviewTopic
import silentorb.imp.intellij.services.getDocumentMetadataService
import silentorb.imp.intellij.ui.misc.ActiveDocumentWatcher
import silentorb.imp.intellij.ui.misc.getDungeonAndErrors
import silentorb.imp.intellij.ui.misc.replacePanelContents
import silentorb.imp.intellij.ui.misc.watchParsed
import silentorb.imp.parsing.general.ParsingErrors
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.imp.parsing.parser.Dungeon
import silentorb.mythic.spatial.Vector2i
import java.awt.GridLayout
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
    val graph: Graph,
    val node: Id?,
    val steps: List<Id>,
    val type: PathKey,
    val timestamp: Long
)

class PreviewContainer(project: Project, contentManager: ContentManager) : JPanel(), Disposable {
  var display: PreviewDisplay? = null
  var previousDocument: Document? = null
  val connection: MessageBusConnection
  var lastDungeon: Dungeon? = null
  var lastErrors: ParsingErrors = listOf()
  var state: PreviewState? = null
  val documentListener: DocumentListener = object : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      val response = getDungeonAndErrors(project, event.document)
      if (response != null) {
        val (dungeon, errors) = response
        update(dungeon, errors)
      }
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
      update(dungeon, errors)
    }

//    println("file change: ${file?.name ?: "none"}")
  })

  init {
    activeDocumentWatcher.start(contentManager)
    layout = GridLayout(0, 1)
    Disposer.register(contentManager, this)

    val bus = ApplicationManager.getApplication().getMessageBus()
    connection = bus.connect()
    connection.subscribe(nodePreviewTopic, object : NodePreviewNotifier {
      override fun handle(document: Document, node: Id?) {
        if (document == previousDocument) {
          update(lastDungeon, lastErrors)
        }
      }
    })
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

    update(this, dungeon, errors, node)
  }

  override fun dispose() {
    val document = previousDocument
    if (document != null) {
      document.removeDocumentListener(documentListener)
    }
    connection.disconnect()
  }
}

fun messagePanel(message: String): JPanel {
  val panel = JPanel()
  panel.add(JLabel(message))
  return panel
}

fun newPreview(type: PathKey, dimensions: Vector2i): PreviewDisplay? {
  val display = previewTypes().get(type)
  return if (display != null)
    display(NewPreviewProps(dimensions))
  else
    null
}

private val sourceLock = ReentrantLock()

fun updatePreviewState(
    type: PathKey,
    graph: Graph,
    timestamp: Long,
    container: PreviewContainer,
    node: Id?
): PreviewState {
  val steps = arrangeGraphSequence(graph)
  sourceLock.lock()
  val state = PreviewState(
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

fun updatePreview(preview: PreviewContainer, graph: Graph, type: PathKey, timestamp: Long, node: Id?) {
  if (type != preview.state?.type) {
    val newDisplay = newPreview(type, Vector2i(preview.width))
    if (newDisplay != null) {
      replacePanelContents(preview, newDisplay.content)
//      preview.setContent(newDisplay.content)
//      preview.toolbar = newDisplay.toolbar?.component ?: JPanel()
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
      val typeName = type.path + "." + type.name
      replacePanelContents(preview, messagePanel("No preview for type of $typeName"))
//      preview.setContent(messagePanel("No preview for type of $typeName"))
    }
  }
  val state = updatePreviewState(type, graph, timestamp, preview, node)
  // The layout of new preview children isn't fully initialized until after this UI tick
  SwingUtilities.invokeLater {
    SwingUtilities.invokeLater {
      val display = preview.display
      if (display != null) {
        if (display.content.width != 0) {
          display.update(state)
        }
      }
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

fun updatePreview(graph: Graph, preview: PreviewContainer, timestamp: Long, node: Id?) {
  if (!trySetPreviewTimestamp(timestamp))
    return

  val output = node ?: getGraphOutputNode(graph)
  val type = graph.types[output]
  if (type != null) {
    updatePreview(preview, graph, type, timestamp, node)
  }
}

fun update(container: PreviewContainer, dungeon: Dungeon?, errors: ParsingErrors, node: Id?) {
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
    updatePreview(dungeon.graph, container, System.currentTimeMillis(), node)
  }
}
