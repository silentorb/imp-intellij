package silentorb.imp.intellij.ui

import com.intellij.ui.layout.selected
import silentorb.mythic.imaging.Bitmap
import silentorb.mythic.imaging.BufferedImageType
import silentorb.mythic.imaging.bitmapToBufferedImage
import silentorb.mythic.spatial.Vector2i
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.*

fun newImagePreview(image: Image): JComponent {
  return JLabel(ImageIcon(image))
}

private var tiling: Boolean = true

fun newImagePreviewChild(fullImage: BufferedImage, dimensions: Vector2i): JComponent {
  val adjustmentX = 512f / dimensions.x
  val adjustmentY = 512f / dimensions.y
  return if (tiling) {
    val image = fullImage.getScaledInstance(
        (dimensions.x * adjustmentX / 3).toInt(),
        (dimensions.y * adjustmentY / 3).toInt()
        , Image.SCALE_DEFAULT)
    val grid = GridLayout(3, 3)
    val gridPanel = JPanel()
    gridPanel.layout = grid
    for (i in 0 until 9) {
      gridPanel.add(newImagePreview(image))
    }
    gridPanel
  } else {
    val image = fullImage.getScaledInstance(
        (dimensions.x * adjustmentX).toInt(),
        (dimensions.y * adjustmentY).toInt()
        , Image.SCALE_DEFAULT)
    newImagePreview(image)
  }
}

fun newImagePreview(bitmap: Bitmap): JComponent {
  val fullImage = bitmapToBufferedImage(bitmap)
  val dimensions = bitmap.dimensions
  val child = newImagePreviewChild(fullImage, dimensions)

  val container = JPanel()
  container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
//  container.add(child)
  val previewWrapper = JPanel()
  container.add(previewWrapper)
  previewWrapper.add(child)
  val toggleTiling = JCheckBox("Tile")
  toggleTiling.isSelected = tiling
  toggleTiling.addItemListener {
    tiling = !tiling
    replacePanelContents(previewWrapper, newImagePreviewChild(fullImage, dimensions))
//    container.remove(0)
//    container.add(newImagePreviewChild(fullImage, dimensions), 0)
//    container.revalidate()
//    container.repaint()
  }
  container.add(toggleTiling)

  return container
}
