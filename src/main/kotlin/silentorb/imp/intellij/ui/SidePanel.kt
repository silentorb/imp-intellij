package silentorb.imp.intellij.ui

import com.intellij.ui.JBSplitter
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.core.mergeNamespaces
import silentorb.imp.execution.execute
import silentorb.imp.intellij.language.initialContext
import silentorb.imp.intellij.language.initialFunctions
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.imp.parsing.parser.Dungeon
import silentorb.imp.parsing.parser.parseText
import javax.swing.JComponent
import javax.swing.JPanel

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

fun updateSidePanel(changePsiValue: ChangePsiValue, sidePanel: SidePanel, content: CharSequence, caretOffset: Int, tracker: ControlTracker?): Pair<Dungeon, ControlTracker?> {
  val preview = sidePanel.previewContainer
  val context = initialContext()
  val (dungeon, errors) = parseText(context)(content)
  if (errors.any()) {
    val errorPanel = messagePanel(formatError(::englishText, errors.first()))
    preview.removeAll()
    preview.add(newPreview(errorPanel))
    preview.revalidate()
    preview.repaint()
  } else {
    val graph = dungeon.graph
    if (graph.nodes.none()) {
      messagePanel("No output to display")
    } else {
      val values = execute(initialFunctions(), graph)
      val output = getGraphOutputNode(graph)
      val value = values[output]

      preview.removeAll()
      preview.add(newPreview(value))
      preview.revalidate()
      preview.repaint()
    }
  }
  val newTracker = updateControlPanel(changePsiValue, mergeNamespaces(context), sidePanel.controls, dungeon, caretOffset, tracker)
  return Pair(dungeon, newTracker)
}
