package silentorb.imp.intellij.ui

import silentorb.imp.core.Id
import silentorb.imp.core.Namespace
import silentorb.imp.core.NumericTypeConstraint
import silentorb.imp.core.PathKey
import silentorb.imp.parsing.general.Range
import silentorb.imp.parsing.general.isInRange
import silentorb.imp.parsing.parser.Dungeon
import silentorb.mythic.imaging.rgbColorKey
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.HeadlessException
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.Serializable
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

typealias ChangePsiValue = (Int, String) -> Unit

data class ControlField(
    val name: String,
    val nodes: List<Id>,
    val tokenOffsets: List<Int>,
    val type: PathKey,
    val valueRange: NumericTypeConstraint?
)

typealias ComplexTypeControl = (ChangePsiValue, List<Int>, List<Any>) -> JComponent

class ColorTracker(val chooser: JColorChooser) : ActionListener {
  var _color: Color? = null
  override fun actionPerformed(e: ActionEvent?) {
    _color = chooser.color
  }

  fun getColor(): Color? {
    return _color
  }
}

fun newColorPicker(changePsiValue: ChangePsiValue, offsets: List<Int>, values: List<Any>): JComponent {
  val colorValues = values as List<Float>
  var color = Color(colorValues[0], colorValues[1], colorValues[2])
  var cancelColor = color
  val colorSample = JButton()
  colorSample.background = color
  colorSample.setPreferredSize(Dimension(30, 30))
  colorSample.border = BorderFactory.createLineBorder(Color.black)
  colorSample.addActionListener {
    cancelColor = color
    val pane = JColorChooser(color)
    val updateColor = { newColor: Color ->
      if (newColor != color) {
        val components = FloatArray(3) { 0f }
        newColor.getColorComponents(components)
        val offsetIterator = offsets.iterator()
        for (component in components) {
          changePsiValue(offsetIterator.next(), component.toString())
        }
        color = newColor
        colorSample.background = color
      }
    }

    pane.selectionModel.addChangeListener {
      updateColor(pane.color)
    }

    val dialog = JColorChooser.createDialog(null, "Choose a Color", true, pane, { }) {
      updateColor(cancelColor)
    }
    dialog.isVisible = true
  }
  return colorSample
}

private val complexTypeControls = mapOf<PathKey, ComplexTypeControl>(
    rgbColorKey to ::newColorPicker
)

fun newFieldControl(namespace: Namespace, dungeon: Dungeon, node: Id): ControlField? {
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
        .map { dungeon.nodeMap[it.source]!!.start.index }

    ControlField(
        name = parameters.first(),
        nodes = nodes,
        tokenOffsets = offsets,
        type = type,
        valueRange = namespace.numericTypeConstraints[type]
    )
  } else
    null
}

fun gatherControlFields(namespace: Namespace, dungeon: Dungeon, node: Id): List<ControlField> {
  val function = dungeon.graph.functionTypes[node]
  return if (function != null && !complexTypeControls.containsKey(function)) {
    val signatureMatch = dungeon.graph.signatureMatches[node]!!
    signatureMatch.alignment.mapNotNull { (_, argumentNode) ->
      newFieldControl(namespace, dungeon, argumentNode)
    }
  } else {
    listOfNotNull(newFieldControl(namespace, dungeon, node))
  }
}

fun newSlider(changePsiValue: ChangePsiValue, offset: Int, initialValue: Int, constraintRange: NumericTypeConstraint, row: JPanel) {
  var value = initialValue.toString()
  val slider = JSlider(SwingConstants.HORIZONTAL, constraintRange.minimum.toInt(), constraintRange.maximum.toInt(), initialValue)
  val textBox = JTextField()
  textBox.text = value.toString()
  val onValueChange = { newValue: String ->
    if (newValue != value) {
      changePsiValue(offset, newValue)
      value = newValue
      if (textBox.text != newValue)
        textBox.text = newValue

      if (slider.value != newValue.toInt())
        slider.value = newValue.toInt()
    }
  }
  slider.addChangeListener { _ ->
    onValueChange(slider.value.toString())
  }
  textBox.addPropertyChangeListener {
    onValueChange(textBox.text)
  }
  row.add(textBox)
  row.add(slider)
}

fun updateControlList(changePsiValue: ChangePsiValue, values: Map<Id, Any>, field: ControlField): JComponent {
  val nodes = field.nodes
  val offsets = field.tokenOffsets

  val row = JPanel()
  val constraintRange = field.valueRange
  if (constraintRange != null) {
    val value = values[nodes.first()]!!
    newSlider(changePsiValue, offsets.first(), value as Int, constraintRange, row)
  } else if (complexTypeControls.containsKey(field.type)) {
    val childValues = nodes.map { values[it]!! }
    row.add(complexTypeControls[field.type]!!(changePsiValue, offsets, childValues))
  }
  val label = JLabel(field.name, SwingConstants.RIGHT)
  label.setPreferredSize(Dimension(80, 30))
  row.add(label)
  return row
}

fun updateControlPanel(changePsiValue: ChangePsiValue, namespace: Namespace, controls: JPanel, dungeon: Dungeon, offset: Int, tracker: ControlTracker?): ControlTracker? {
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
      val fields = gatherControlFields(namespace, dungeon, node)
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
