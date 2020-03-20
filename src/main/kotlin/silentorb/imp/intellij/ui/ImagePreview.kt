package silentorb.imp.intellij.ui

import com.intellij.ide.util.PropertiesComponent
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

private const val tilingConfigKey = "silentorb.imp.intellij.config.tiling"
fun getTiling():Boolean =
    PropertiesComponent.getInstance().getBoolean(tilingConfigKey, false)

fun setTiling(value: Boolean) {
  PropertiesComponent.getInstance().setValue(tilingConfigKey, value)
}

fun newImagePreviewChild(fullImage: BufferedImage, dimensions: Vector2i): JComponent {
  val adjustmentX = 512f / dimensions.x
  val adjustmentY = 512f / dimensions.y
  return if (getTiling()) {
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
  val previewWrapper = JPanel()
  container.add(previewWrapper)
  previewWrapper.add(child)
  val toggleTiling = JCheckBox("Tile")
  toggleTiling.isSelected = getTiling()
  toggleTiling.addItemListener {
    setTiling(!getTiling())
    replacePanelContents(previewWrapper, newImagePreviewChild(fullImage, dimensions))
  }
  container.add(toggleTiling)

  return container
}
