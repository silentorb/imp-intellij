package silentorb.imp.intellij.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.ContentManager
import com.intellij.util.messages.MessageBusConnection
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.core.PathKey
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.intellij.messaging.NodePreviewNotifier
import silentorb.imp.intellij.messaging.nodePreviewTopic
import silentorb.imp.intellij.services.getDocumentMetadataService
import silentorb.imp.parsing.general.ParsingErrors
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.imp.parsing.parser.Dungeon
import silentorb.mythic.imaging.floatSamplerKey
import silentorb.mythic.imaging.rgbSamplerKey
import silentorb.mythic.spatial.Vector2i
import java.awt.GridLayout
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

data class PreviewDisplay(
    val component: JComponent,
    val update: (PathKey, Graph, Long, Id?) -> Unit = { _, _, _, _ -> }
)

class PreviewContainer(project: Project, contentManager: ContentManager) : JPanel(), Disposable {
  var type: PathKey? = null
  var display: PreviewDisplay? = null
  var previousDocument: Document? = null
  val connection: MessageBusConnection
  var lastDungeon: Dungeon? = null
  var lastErrors: ParsingErrors = listOf()
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

fun newPreview(type: PathKey, dimensions: Vector2i): PreviewDisplay {
  return when (type) {
    rgbSamplerKey -> newImagePreview(dimensions)
    floatSamplerKey -> newImagePreview(dimensions)
    else -> {
      val typeName = type.path + "." + type.name

      PreviewDisplay(messagePanel("No preview for type of $typeName"))
    }
  }
}

fun updatePreview(preview: PreviewContainer, graph: Graph, type: PathKey, timestamp: Long, node: Id?) {
  if (type != preview.type) {
    preview.type = type
    val newDisplay = newPreview(type, Vector2i(preview.width))
    val component = newDisplay.component
    replacePanelContents(preview, component)
    val oldComponent = preview.display?.component
    if (oldComponent is Disposable) {
      Disposer.dispose(oldComponent)
    }
    preview.display = newDisplay
    if (component is Disposable) {
      Disposer.register(preview, component)
    }
  }
  // The layout of new preview children isn't fully initialized until after this UI tick
  SwingUtilities.invokeLater {
    val display = preview.display
    if (display != null) {
      display.update(type, graph, timestamp, node)
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
    container.type = null
    container.display = null
    container.removeAll()
    container.revalidate()
    container.repaint()
  } else if (errors.any()) {
    container.type = null
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
