package silentorb.imp.intellij.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.round

fun newColorPicker(changePsiValue: ChangePsiValue, psiElements: List<PsiElementWrapper>, values: List<Any>): JComponent {
  val colorValues = values as List<Int>
  var color = Color(colorValues[0], colorValues[1], colorValues[2])
  var cancelColor = color
  val colorSample = JPanel()
  colorSample.background = color
  colorSample.setPreferredSize(Dimension(30, 30))
  colorSample.border = BorderFactory.createLineBorder(Color.black)
  colorSample.addMouseListener(object : MouseListener {
    override fun mouseClicked(e: MouseEvent?) {
      cancelColor = color
      val pane = JColorChooser(color)
      pane.removeChooserPanel(pane.chooserPanels.first())
      val updateColor = { newColor: Color ->
        if (newColor != color) {
          val components = listOf(newColor.red, newColor.green, newColor.blue)
          val offsetIterator = psiElements.iterator()
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

    override fun mouseReleased(e: MouseEvent?) {
    }

    override fun mouseEntered(e: MouseEvent?) {
    }

    override fun mouseExited(e: MouseEvent?) {
    }

    override fun mousePressed(e: MouseEvent?) {
    }
  })
  return colorSample
}
