package silentorb.imp.intellij.ui

import com.intellij.ui.JBSplitter
import silentorb.imp.core.FunctionKey
import silentorb.imp.core.Graph
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.core.mergeNamespaces
import silentorb.imp.execution.OutputValues
import silentorb.imp.execution.arrangeGraphSequence
import silentorb.imp.execution.executeStep
import silentorb.imp.intellij.language.initialContext
import silentorb.imp.intellij.language.initialFunctions
import silentorb.imp.intellij.language.scaleLengthKey
import silentorb.imp.intellij.language.scaleLengthSignature
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.imp.parsing.parser.Dungeon
import silentorb.imp.parsing.parser.parseText
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread


data class SidePanel(
    val root: JComponent,
    val previewContainer: JPanel,
    val controls: JPanel
)

fun newSidePanel(): SidePanel {
  val splitter = JBSplitter(true, 0.6f, 0.20f, 0.80f)
  val previewContainer = JPanel()
  val controlList = newControlPanel()
  splitter.firstComponent = previewContainer
  splitter.secondComponent = controlList
  splitter.dividerWidth = 3
  return SidePanel(
      root = splitter,
      previewContainer = previewContainer,
      controls = controlList
  )
}

data class PreviewState(
    var graph: Int,
    var scale: Float
)

private val previewState: PreviewState = PreviewState(
    graph = 0,
    scale = 0f
)

private var previewThreadCount: Int = 0

private val lock = ReentrantLock()

fun processGraphCancelable(graph: Graph, scale: Float): OutputValues? {
  val functions = initialFunctions()
      .plus(FunctionKey(scaleLengthKey, scaleLengthSignature) to { arguments ->
        val length = arguments["length"] as Int
        (length.toFloat() * scale).toInt()
      })
  val graphHashCode = graph.hashCode()
  val steps = arrangeGraphSequence(graph)
  var values: OutputValues = mapOf()
  var i = 1
  for (step in steps) {
    if (previewState.graph != graphHashCode) {
      println("Cancelling on step $i")
      return null
    }
    values = executeStep(functions, graph)(values, step)
    ++i
  }
  return values
}

private val lock2 = ReentrantLock()

fun updatePreview(graph: Graph, preview: JPanel, scale: Float) {
  val graphHashCode = graph.hashCode()
  thread(start = true) {
    lock2.lock()
    ++previewThreadCount
    lock2.unlock()
    println("Thread count inc: $previewThreadCount")
    val values = processGraphCancelable(graph, scale)
    val output = getGraphOutputNode(graph)
    if (values != null) {
      val value = values[output]

      lock.lock()
      if (previewState.graph == graphHashCode && scale >= previewState.scale) {
        previewState.scale = scale
        SwingUtilities.invokeLater {
          replacePanelContents(preview, newPreview(value))
        }
      }
      lock.unlock()
    }
    lock2.lock()
    --previewThreadCount
    lock2.unlock()
    println("Thread count dec: $previewThreadCount")
  }
}

fun updateSidePanel(getPsiElement: GetPsiValue, changePsiValue: ChangePsiValue, sidePanel: SidePanel, content: CharSequence, caretOffset: Int, tracker: ControlTracker?): Pair<Dungeon, ControlTracker?> {
  val preview = sidePanel.previewContainer
  val context = initialContext()
  val (dungeon, errors) = parseText(context)(content)
  if (errors.any()) {
    val errorPanel = messagePanel(formatError(::englishText, errors.first()))
    preview.removeAll()
    preview.add(errorPanel)
    preview.revalidate()
    preview.repaint()
  } else {
    val graph = dungeon.graph
    if (graph.nodes.none()) {
      messagePanel("No output to display")
    } else {
      previewState.graph = graph.hashCode()
      previewState.scale = 0f
      updatePreview(graph, preview, 1f)
//      updatePreview(graph, preview, 0.25f)
    }
  }
  val newTracker = updateControlPanel(getPsiElement, changePsiValue, mergeNamespaces(context), sidePanel.controls, dungeon, caretOffset, tracker)
  return Pair(dungeon, newTracker)
}
