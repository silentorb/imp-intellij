package silentorb.imp.intellij.ui

import com.intellij.ide.util.PropertiesComponent
import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.core.PathKey
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.arrangeGraphSequence
import silentorb.imp.execution.execute
import silentorb.imp.intellij.language.initialFunctions
import silentorb.mythic.imaging.*
import silentorb.mythic.spatial.Vector2i
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.concurrent.locks.ReentrantLock
import javax.swing.*
import kotlin.concurrent.thread

data class ImageArtifact(
    val data: BufferedImage
)

private val gridLock = ReentrantLock()

data class ImagePreviewState(
    val graph: Graph,
    val steps: List<Id>,
    val type: PathKey,
    val timestamp: Long
)

class ImagePreviewPanel(dimensions: Vector2i) : JPanel() {
  var grid = newImagePreviewChild(dimensions)
  var state: ImagePreviewState? = null
}

//private val dimensions = Vector2i(512, 512)

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

fun newImagePreviewChild(dimensions: Vector2i): JPanel {
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

fun fillImageGrid(dimensions: Vector2i, grid: JPanel, location: Vector2i, fullImage: BufferedImage) {
  val repetitions = getRepetitions()
  val divisions = repetitions * cellCount
  val divisionDimensions = dimensions / divisions
  val image = fullImage.getScaledInstance(divisionDimensions.x, divisionDimensions.y, Image.SCALE_DEFAULT)
  val scalar1 = cellCount.x
  val scalar2 = scalar1 * repetitions.x
  val scalar3 = scalar2 * cellCount.y
  for (repetitionY in 0 until repetitions.y) {
    for (repetitionX in 0 until repetitions.x) {
      val index = location.x +
          scalar1 * repetitionX +
          scalar2 * location.y +
          scalar3 * repetitionY
      grid.remove(index)
      grid.add(newImageElement(image), index)
    }
  }
}

fun updateImagePreview(state: ImagePreviewState, container: ImagePreviewPanel, dimensions: Vector2i) {
  val timestamp = state.timestamp
  val graph = state.graph
  val steps = state.steps
  val type = state.type

  if (isPreviewOutdated(timestamp))
    return

  val cellCoordinates = (0 until cellCount.y).flatMap { y ->
    (0 until cellCount.x).map { x ->
      Vector2i(x, y)
    }
  }

  val cellDimensions = dimensions / cellCount
  val functions = initialFunctions()

  thread(start = true) {
//    var i = 0
    for (cellCoordinate in cellCoordinates) {
      if (isPreviewOutdated(timestamp)) {
//        println("$timestamp Canceled1")
        break
      }
      val values = execute(functions, graph, steps)
      val output = getGraphOutputNode(graph)
      val value = values[output]
      if (value == null)
        break

      val sampleWriter = if (type == rgbSamplerKey)
        newRgbSampleWriter(value as RgbSampler)
      else
        newFloatSampleWriter(value as FloatSampler)

      val image = newBufferedImage(cellDimensions, sampleWriter.depth)
      samplerToBufferedImage(sampleWriter, image, dimensions,
          cellCoordinate * cellDimensions, cellDimensions
      )

      if (isPreviewOutdated(timestamp)) {
//        println("$timestamp Canceled2")
        break
      }
      SwingUtilities.invokeLater {
        gridLock.lock()
//        println("$timestamp Drawing ${i++}")
        fillImageGrid(dimensions, container.grid, cellCoordinate, image)
        container.grid.revalidate()
        container.grid.repaint()
        gridLock.unlock()
      }
    }
  }
}

private val sourceLock = ReentrantLock()

fun updateImagePreview(type: PathKey, graph: Graph, timestamp: Long, container: ImagePreviewPanel, dimensions: Vector2i) {
  val steps = arrangeGraphSequence(graph)
  sourceLock.lock()
  val state = ImagePreviewState(
      type = type,
      graph = graph,
      steps = steps,
      timestamp = timestamp
  )
  container.state = state
  sourceLock.unlock()
  updateImagePreview(state, container, dimensions)
}

fun newImagePreview(dimensions: Vector2i): PreviewDisplay {
  val container = ImagePreviewPanel(dimensions)
  container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
  val previewWrapper = JPanel()
  previewWrapper.add(container.grid)
  container.add(previewWrapper)
  val toggleTiling = JCheckBox("Tile")
  toggleTiling.isSelected = getTiling()
  toggleTiling.addItemListener {
    setTiling(!getTiling())
    val grid = newImagePreviewChild(dimensions)
    container.grid = grid
    replacePanelContents(previewWrapper, grid)
    sourceLock.lock()
    val state = container.state
    sourceLock.unlock()
    if (state != null) {
      updateImagePreview(state, container, dimensions)
    }
  }
  container.add(toggleTiling)

  return PreviewDisplay(
      component = container,
      update = { type, graph, timestamp ->
        updateImagePreview(type, graph, timestamp, container, dimensions)
      }
  )
}
