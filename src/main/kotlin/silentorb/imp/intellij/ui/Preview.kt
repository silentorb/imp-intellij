package silentorb.imp.intellij.ui

import silentorb.mythic.imaging.Bitmap
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

fun messagePanel(message: String): JPanel {
  val panel = JPanel()
  panel.add(JLabel(message))
  return panel
}

fun newPreview(value: Any?): JComponent {
  return when (value) {
    is Bitmap -> newImagePreview(value)
    else -> {
      val typeName = if (value == null)
        "null"
      else
        value.javaClass.name

      messagePanel("No preview for type of $typeName")
    }
  }
}
