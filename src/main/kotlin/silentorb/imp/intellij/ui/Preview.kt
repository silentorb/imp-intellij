package silentorb.imp.intellij.ui

import com.intellij.openapi.project.Project
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.core.PathKey
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.execute
import silentorb.imp.intellij.language.initialFunctions
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.mythic.imaging.*
import silentorb.mythic.spatial.Vector2i
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

data class PreviewDisplay(
    val component: JComponent,
    val update: (PathKey, Graph, Long) -> Unit = { _, _, _ -> }
)

class PreviewContainer(project: Project) : JPanel() {
  var type: PathKey? = null
  var display: PreviewDisplay? = null
  var node: Id? = null
  val activeDocumentWatcher = ActiveDocumentWatcher(project, this, watchParsed { dungeon, errors ->
    if (dungeon == null) {
      removeAll()
      revalidate()
      repaint()
    }
    else if (errors.any()){
      val errorPanel = messagePanel(formatError(::englishText, errors.first()))
      removeAll()
      add(errorPanel)
      revalidate()
      repaint()
    }
    else {
      updatePreview(dungeon.graph, this, System.currentTimeMillis())
    }
//    println("file change: ${file?.name ?: "none"}")
  })

  init {
    activeDocumentWatcher.start()
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

fun updatePreview(preview: PreviewContainer, graph: Graph, type: PathKey, timestamp: Long) {
  if (type != preview.type) {
    preview.type = type
    val newDisplay = newPreview(type, Vector2i(preview.width))
    replacePanelContents(preview, newDisplay.component)
    preview.display = newDisplay
  }
  val display = preview.display
  if (display != null) {
    display.update(type, graph, timestamp)
  }
}

private var previewThreadCount: Int = 0

private val lock = ReentrantLock()

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

fun updatePreview(graph: Graph, preview: PreviewContainer, timestamp: Long) {
  if (!trySetPreviewTimestamp(timestamp))
    return

  val output = getGraphOutputNode(graph)
  val type = graph.types[output]
  if (type != null) {
    updatePreview(preview, graph, type, timestamp)
  }
//  thread(start = true) {
//    timestampLock.lock()
//    ++previewThreadCount
//    timestampLock.unlock()
////    println("Thread count inc: $previewThreadCount")
//
//    val output = getGraphOutputNode(graph)
//    val type = graph.types[output]
//    if (type != null) {
//
//      lock.lock()
//      SwingUtilities.invokeLater {
//        updatePreview(preview, graph, type, timestamp)
//      }
//      lock.unlock()
//    }
//    timestampLock.lock()
//    --previewThreadCount
//    timestampLock.unlock()
////    println("Thread count dec: $previewThreadCount")
//  }
}
