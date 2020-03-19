package silentorb.imp.intellij.ui

import com.intellij.psi.PsiElement
import silentorb.imp.core.Id
import silentorb.imp.core.Namespace
import silentorb.imp.core.NumericTypeConstraint
import silentorb.imp.core.PathKey
import silentorb.imp.parsing.general.Range
import silentorb.imp.parsing.general.isInRange
import silentorb.imp.parsing.parser.Dungeon
import silentorb.mythic.imaging.rgbColorKey
import java.awt.Dimension
import javax.swing.*

fun newControlPanel(): JPanel {
  val panel = JPanel()
  panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
  return panel
}

data class ControlTracker(
    val node: Id,
    val range: Range
)

fun clearControlList(controls: JPanel) {
  controls.removeAll()
  controls.revalidate()
  controls.repaint()
}

data class PsiElementWrapper(
    var element: PsiElement
)

typealias GetPsiValue = (Int) -> PsiElementWrapper?
typealias ChangePsiValue = (PsiElementWrapper, String) -> Unit

data class ControlField(
    val name: String,
    val nodes: List<Id>,
    val psiElements: List<PsiElementWrapper>,
    val type: PathKey,
    val valueRange: NumericTypeConstraint?
)

typealias ComplexTypeControl = (ChangePsiValue, List<PsiElementWrapper>, List<Any>) -> JComponent

private val complexTypeControls = mapOf<PathKey, ComplexTypeControl>(
    rgbColorKey to ::newColorPicker
)

fun newFieldControl(getPsiElement: GetPsiValue, namespace: Namespace, dungeon: Dungeon, node: Id): ControlField? {
  val functionType = dungeon.graph.functionTypes[node]
  val complexTypeControl = complexTypeControls[functionType]
  val type = dungeon.literalConstraints[node]
      ?: if (complexTypeControl != null)
        functionType
      else
        null

  val parameters = dungeon.graph.signatureMatches.values.flatMap { match ->
    match.alignment.filter { it.value == node }.keys
  }
  return if (type != null && parameters.any()) {
    val nodes = if (complexTypeControl != null) {
      dungeon.graph.connections
          .filter { it.destination == node }
          .map { it.source }
    } else
      listOf(node)

    val offsets = dungeon.graph.connections
        .filter { it.destination == node }
        .map { it.source }
        .plus(node)
        .map { dungeon.nodeMap[it]!!.start.index }

    val psiElements = offsets.map(getPsiElement).filterNotNull()
    if (psiElements.any())
      ControlField(
          name = parameters.first(),
          nodes = nodes,
          psiElements = psiElements,
          type = type,
          valueRange = namespace.numericTypeConstraints[type]
      )
    else
      null
  } else
    null
}

fun gatherControlFields(getPsiElement: GetPsiValue, namespace: Namespace, dungeon: Dungeon, node: Id): List<ControlField> {
  val function = dungeon.graph.functionTypes[node]
  return if (function != null && !complexTypeControls.containsKey(function)) {
    val signatureMatch = dungeon.graph.signatureMatches[node]!!
    signatureMatch.alignment.mapNotNull { (_, argumentNode) ->
      newFieldControl(getPsiElement, namespace, dungeon, argumentNode)
    }
  } else {
    listOfNotNull(newFieldControl(getPsiElement, namespace, dungeon, node))
  }
}

fun updateControlList(changePsiValue: ChangePsiValue, values: Map<Id, Any>, field: ControlField): JComponent {
  val nodes = field.nodes
  val psiElements = field.psiElements

  val row = JPanel()
  val constraintRange = field.valueRange
  if (constraintRange != null) {
    val value = values[nodes.first()]!!
    newSlider(changePsiValue, psiElements.first(), value as Int, constraintRange, row)
  } else if (complexTypeControls.containsKey(field.type)) {
    val childValues = nodes.map { values[it]!! }
    row.add(complexTypeControls[field.type]!!(changePsiValue, psiElements, childValues))
  }
  val label = JLabel(field.name, SwingConstants.RIGHT)
  label.setPreferredSize(Dimension(80, 30))
  row.add(label)
  return row
}

fun updateControlPanel(getPsiElement: GetPsiValue, changePsiValue: ChangePsiValue, namespace: Namespace, controls: JPanel, dungeon: Dungeon, offset: Int, tracker: ControlTracker?): ControlTracker? {
  val nodeRange = dungeon.nodeMap.entries
      .firstOrNull { (_, range) -> isInRange(range, offset) }

  val node = nodeRange?.key

  return if (node != null) {
    val newTracker = ControlTracker(
        node = node,
        range = nodeRange.value
    )
    if (tracker != newTracker) {
      clearControlList(controls)
      val fields = gatherControlFields(getPsiElement, namespace, dungeon, node)
      fields.forEach { field ->
        controls.add(updateControlList(changePsiValue, dungeon.graph.values, field))
      }
    }
    newTracker
  } else {
    if (controls.componentCount > 0)
      clearControlList(controls)
    null
  }
}
