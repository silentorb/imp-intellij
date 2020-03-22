package silentorb.imp.intellij.ui

import com.intellij.ui.JBSplitter
import silentorb.imp.core.mergeNamespaces
import silentorb.imp.intellij.language.initialContext
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.general.formatError
import silentorb.imp.parsing.parser.Dungeon
import silentorb.imp.parsing.parser.parseText
import javax.swing.JComponent
import javax.swing.JPanel

data class SidePanel(
    val root: JComponent,
    val previewContainer: PreviewContainer,
    val controls: JPanel
)

fun newSidePanel(): SidePanel {
  val splitter = JBSplitter(true, 0.6f, 0.20f, 0.80f)
  val previewContainer = PreviewContainer()
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

fun updateSidePanel(getPsiElement: GetPsiValue, changePsiValue: ChangePsiValue, sidePanel: SidePanel, content: CharSequence, caretOffset: Int, tracker: ControlTracker?): Pair<Dungeon, ControlTracker?> {
  val preview = sidePanel.previewContainer
  val context = initialContext()
  val timestamp = System.currentTimeMillis()
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
      updatePreview(graph, preview, timestamp)
    }
  }
  val newTracker = updateControlPanel(getPsiElement, changePsiValue, mergeNamespaces(context), sidePanel.controls, dungeon, caretOffset, tracker)
  return Pair(dungeon, newTracker)
}
