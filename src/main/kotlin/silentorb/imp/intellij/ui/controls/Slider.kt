package silentorb.imp.intellij.ui.controls

import silentorb.imp.core.NumericTypeConstraint
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JTextField
import javax.swing.SwingConstants

fun newSlider(changePsiValue: ChangePsiValue, psiElement: PsiElementWrapper, initialValue: Int, constraintRange: NumericTypeConstraint, row: JPanel) {
  var value = initialValue.toString()
  val slider = JSlider(SwingConstants.HORIZONTAL, constraintRange.minimum.toInt(), constraintRange.maximum.toInt(), initialValue)
  val textBox = JTextField()
  textBox.text = value.toString()
  val onValueChange = { newValue: String ->
    if (newValue != value) {
      changePsiValue(psiElement, newValue)
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
