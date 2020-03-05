package silentorb.imp.intellij.ui

import com.intellij.util.ui.ImageUtil
import silentorb.mythic.imaging.Bitmap
import silentorb.mythic.imaging.bitmapToBufferedImage
import java.awt.GridLayout
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_3BYTE_BGR
import java.awt.image.BufferedImage.TYPE_BYTE_GRAY
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

fun newImagePreview(image: Image): JComponent {
  return JLabel(ImageIcon(image))
}

fun newImagePreview(bitmap: Bitmap, tiled: Boolean = true): JComponent {
  val fullImage = bitmapToBufferedImage(bitmap)
  return if (tiled) {
    val dimensions = bitmap.dimensions
    val image = fullImage.getScaledInstance(dimensions.x / 3, dimensions.y / 3, Image.SCALE_DEFAULT)
    val grid = GridLayout(3, 3)
    val panel = JPanel()
    panel.layout = grid
    for (i in 0 until 9) {
      panel.add(newImagePreview(image))
    }
    panel
  }
  else {
    newImagePreview(fullImage)
  }
}
