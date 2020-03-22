package silentorb.imp.intellij.ui

import com.intellij.ide.util.PropertiesComponent
import silentorb.imp.core.PathKey
import silentorb.mythic.imaging.*
import silentorb.mythic.spatial.Vector2i
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.*

data class ImageArtifact(
    val data: BufferedImage
)

typealias CellImageMap = Map<Vector2i, ImageArtifact>

class ImagePreviewPanel : JPanel() {
  var grid = newImagePreviewChild()
  var images: CellImageMap = mapOf()
  var source: Any? = null
  var sourceType: PathKey? = null
}

private val dimensions = Vector2i(512, 512)

val cellCount = Vector2i(4, 4)

fun getRepetitions() =
    if (getTiling())
      Vector2i(3, 3)
    else
      Vector2i(1, 1)

private const val tilingConfigKey = "silentorb.imp.intellij.config.tiling"

fun getTiling(): Boolean =
    PropertiesComponent.getInstance().getBoolean(tilingConfigKey, false)

fun setTiling(value: Boolean) {
  PropertiesComponent.getInstance().setValue(tilingConfigKey, value)
}

fun newImageElement(image: Image): JComponent {
  return JLabel(ImageIcon(image))
}

fun newImagePreviewChild(): JPanel {
  val repetitions = getRepetitions()
  val divisions = repetitions * cellCount
  val grid = JPanel()
  grid.layout = GridLayout(divisions.x, divisions.y)
  val divisionDimensions = dimensions / divisions
  val divisionSize = Dimension(divisionDimensions.x, divisionDimensions.y)
  for (i in 0 until divisions.x * divisions.y) {
    val cell = JPanel()
    cell.preferredSize = divisionSize
    grid.add(cell)
  }
  return grid
}

fun fillImageGrid(grid: JPanel, location: Vector2i, fullImage: BufferedImage) {
  val repetitions = getRepetitions()
  val divisions = repetitions * cellCount
  val divisionDimensions = dimensions / divisions
  val image = fullImage.getScaledInstance(divisionDimensions.x, divisionDimensions.y, Image.SCALE_DEFAULT)
  for (y in 0 until repetitions.y) {
    for (x in 0 until repetitions.x) {
      val index = x * repetitions.x + location.x + y * repetitions.y * repetitions.x + location.y * cellCount.x
      grid.remove(index)
      grid.add(newImageElement(image), index)
      val k = 0
    }
  }
}

fun updateImagePreview(type: PathKey, value: Any, container: ImagePreviewPanel) {
  val sampleWriter = if (type == rgbSamplerKey)
    newRgbSampleWriter(value as RgbSampler)
  else
    newFloatSampleWriter(value as FloatSampler)

  val cellCoordinates = (0 until cellCount.y).flatMap { y ->
    (0 until cellCount.x).map { x ->
      Vector2i(x, y)
    }
  }

  val cellDimensions = dimensions / cellCount

  for (cellCoordinate in cellCoordinates) {
    val partialImage = rgbSamplerToBufferedImage(dimensions, sampleWriter,
        cellCoordinate * cellDimensions, cellDimensions
    )
    fillImageGrid(container.grid, cellCoordinate, partialImage)
  }
}

fun newImagePreview(): PreviewDisplay {
  val container = ImagePreviewPanel()
  container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
  val previewWrapper = JPanel()
  previewWrapper.add(container.grid)
  container.add(previewWrapper)
  val toggleTiling = JCheckBox("Tile")
  toggleTiling.isSelected = getTiling()
  toggleTiling.addItemListener {
    setTiling(!getTiling())
    val grid = newImagePreviewChild()
    container.grid = grid
    replacePanelContents(previewWrapper, grid)
    val source = container.source
    val sourceType = container.sourceType
    if (source != null && sourceType != null) {
      updateImagePreview(sourceType, source, container)
    }
  }
  container.add(toggleTiling)

  return PreviewDisplay(
      component = container,
      update = { type, value ->
        container.sourceType = type
        container.source = value
        updateImagePreview(type, value, container)
      }
  )
}
