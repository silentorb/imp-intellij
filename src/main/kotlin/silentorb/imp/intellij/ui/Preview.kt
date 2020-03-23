package silentorb.imp.intellij.ui

import silentorb.imp.core.Graph
import silentorb.imp.core.PathKey
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.execute
import silentorb.imp.intellij.language.initialFunctions
import silentorb.mythic.imaging.*
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

class PreviewContainer : JPanel() {
  var type: PathKey? = null
  var display: PreviewDisplay? = null
}

fun messagePanel(message: String): JPanel {
  val panel = JPanel()
  panel.add(JLabel(message))
  return panel
}

fun newPreview(type: PathKey): PreviewDisplay {
  return when (type) {
    rgbSamplerKey -> newImagePreview()
    floatSamplerKey -> newImagePreview()
    else -> {
      val typeName = type.path + "." + type.name

      PreviewDisplay(messagePanel("No preview for type of $typeName"))
    }
  }
}

fun updatePreview(preview: PreviewContainer, graph: Graph, type: PathKey, timestamp: Long) {
  if (type != preview.type) {
    preview.type = type
    val newDisplay = newPreview(type)
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

  thread(start = true) {
    timestampLock.lock()
    ++previewThreadCount
    timestampLock.unlock()
//    println("Thread count inc: $previewThreadCount")

    val output = getGraphOutputNode(graph)
    val type = graph.types[output]
    if (type != null) {

      lock.lock()
      SwingUtilities.invokeLater {
        updatePreview(preview, graph, type, timestamp)
      }
      lock.unlock()
    }
    timestampLock.lock()
    --previewThreadCount
    timestampLock.unlock()
//    println("Thread count dec: $previewThreadCount")
  }
}
