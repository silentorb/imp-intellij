package silentorb.imp.intellij.ui

import silentorb.imp.core.Graph
import silentorb.imp.core.PathKey
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.execute
import silentorb.imp.intellij.language.initialFunctions
import silentorb.mythic.imaging.*
import silentorb.mythic.imaging.operators.samplertoBitmap
import silentorb.mythic.imaging.operators.withBitmapBuffer
import silentorb.mythic.imaging.operators.withGrayscaleBuffer
import silentorb.mythic.spatial.Vector2i
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

data class PreviewDisplay(
    val component: JComponent,
    val update: (PathKey, Any) -> Unit = { _, _ -> }
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

fun updatePreview(graph: Graph, preview: PreviewContainer, type: PathKey, value: Any) {
  if (type != preview.type) {
    val newDisplay = newPreview(type)
    replacePanelContents(preview, newDisplay.component)
    preview.display = newDisplay
  }
  val display = preview.display
  if (display != null) {
    display.update(type, value)
  }
}

private var previewThreadCount: Int = 0

private val lock = ReentrantLock()

private val lock2 = ReentrantLock()

fun updatePreview(graph: Graph, preview: PreviewContainer) {
  val graphHashCode = graph.hashCode()
  thread(start = true) {
    lock2.lock()
    ++previewThreadCount
    lock2.unlock()
    println("Thread count inc: $previewThreadCount")
    val functions = initialFunctions()
    val values = execute(functions, graph)
    val output = getGraphOutputNode(graph)
    val value = values[output]
    val type = graph.types[output]
    if (value != null && type != null) {

      lock.lock()
      SwingUtilities.invokeLater {
        updatePreview(graph, preview, type, value)
      }
      lock.unlock()
    }
    lock2.lock()
    --previewThreadCount
    lock2.unlock()
    println("Thread count dec: $previewThreadCount")
  }
}
