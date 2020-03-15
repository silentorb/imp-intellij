package silentorb.imp.intellij.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import silentorb.imp.core.Id
import silentorb.imp.core.Namespace
import silentorb.imp.core.PathKey
import silentorb.imp.intellij.language.ImpLanguage
import silentorb.imp.parsing.general.Range
import silentorb.imp.parsing.general.isInRange
import silentorb.imp.parsing.parser.Dungeon
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.SwingConstants
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

fun newControlPanel(): JPanel {
  return JPanel()
}

data class ControlTracker(
    val node: Id,
    val range: Range,
    val constraintType: PathKey
)

fun clearControlList(controls: JPanel) {
  controls.removeAll()
  controls.revalidate()
  controls.repaint()
}

//typealias GetPsiFile = () -> PsiFile?
//typealias GetPsiFactory = () -> PsiFileFactory

typealias ChangePsiValue = (Int, String) -> Unit

fun updateControlList(
    changePsiValue: ChangePsiValue,
    namespace: Namespace,
    controls: JPanel,
    dungeon: Dungeon,
    node: Id,
    offset: Int,
    constraintType: PathKey
) {
  val constraintRange = namespace.numericTypeConstraints[constraintType]!!
  val value = dungeon.graph.values[node]!!
  val slider = JSlider(SwingConstants.HORIZONTAL, constraintRange.minimum.toInt(), constraintRange.maximum.toInt(), value as Int)
  slider.addChangeListener { event ->
    val newValue = slider.value.toString()
    changePsiValue(offset, newValue)
  }
  controls.add(slider)
}

fun updateControlPanel(changePsiValue: ChangePsiValue, namespace: Namespace, controls: JPanel, dungeon: Dungeon, offset: Int, tracker: ControlTracker?): ControlTracker? {
  val nodeRange = dungeon.nodeMap.entries
      .firstOrNull { (_, range) -> isInRange(range, offset) }

  val node = nodeRange?.key
  val constraintType = dungeon.literalConstraints[node]
  return if (node != null && constraintType != null) {
    val newTracker = ControlTracker(
        node = node,
        range = nodeRange.value,
        constraintType = constraintType
    )
    if (tracker != newTracker) {
      clearControlList(controls)
      updateControlList(changePsiValue, namespace, controls, dungeon, node, nodeRange.value.start.index, constraintType)
    }
    newTracker
  } else {
    if (controls.componentCount > 0)
      clearControlList(controls)
    null
  }
}
