package silentorb.imp.intellij.ui

import com.intellij.util.ui.ImageUtil
import silentorb.mythic.imaging.Bitmap
import silentorb.mythic.spatial.Vector2i
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_3BYTE_BGR
import java.awt.image.BufferedImage.TYPE_BYTE_GRAY
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel

fun newImage(dimensions: Vector2i, buffer: ByteBuffer): BufferedImage {
  buffer.rewind()
  val byteArray = ByteArray(buffer.capacity())
  buffer.get(byteArray)
  val image = ImageUtil.createImage(dimensions.x, dimensions.y, TYPE_3BYTE_BGR)
  ImageIO.read(ByteArrayInputStream(byteArray))
//    image.raster.setPixels(0, 0, dimensions.x, dimensions.y,        byteArray)
//    image.pixelWriter.setPixels(0, 0, dimensions.x, dimensions.y,
//        PixelFormat.getByteRgbInstance(),
//        byteArray, 0, dimensions.x * 3)
  return image
}

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
//  buffer.get(array)
  val image = ImageUtil.createImage(dimensions.x, dimensions.y, type)
  image.raster.setPixels(0, 0, dimensions.x, dimensions.y, array)
//    image.pixelWriter.setPixels(0, 0, dimensions.x, dimensions.y,
//        PixelFormat.getByteRgbInstance(),
//        byteArray, 0, dimensions.x * 3)
  return image
}

fun newImagePreview(image: BufferedImage): JComponent {
  return JLabel(ImageIcon(image))
}

fun newImagePreview(bitmap: Bitmap): JComponent {
//    val image = newImage(bitmap.dimensions, rgbFloatToBytes(bitmap.buffer))
  val image = newImage(bitmap)
  return newImagePreview(image)
}
