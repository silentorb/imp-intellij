package silentorb.imp.intellij.ui

import com.intellij.util.ui.ImageUtil
import silentorb.mythic.imaging.Bitmap
import java.awt.GridLayout
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_3BYTE_BGR
import java.awt.image.BufferedImage.TYPE_BYTE_GRAY
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

fun newImage(bitmap: Bitmap): BufferedImage {
  val buffer = bitmap.buffer
  val dimensions = bitmap.dimensions
  val type = when (bitmap.channels) {
    1 -> TYPE_BYTE_GRAY
    3 -> TYPE_3BYTE_BGR
    else -> throw Error("Unsupported bitmap channel count ${bitmap.channels}")
  }
  buffer.rewind()
  val array = IntArray(buffer.capacity())
  for (i in 0 until buffer.capacity()) {
    array[i] = (buffer.get() * 255).toInt()
  }
  val image = ImageUtil.createImage(dimensions.x, dimensions.y, type)
  image.raster.setPixels(0, 0, dimensions.x, dimensions.y, array)
  return image
}

fun newImagePreview(image: Image): JComponent {
  return JLabel(ImageIcon(image))
}

fun newImagePreview(bitmap: Bitmap, tiled: Boolean = true): JComponent {
  val fullImage = newImage(bitmap)
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
