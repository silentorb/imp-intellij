package silentorb.imp.intellij.ui.texturing

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.messages.MessageBusConnection
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.OutputValues
import silentorb.imp.execution.executeStep
import silentorb.imp.execution.executeToSingleValue
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.messaging.ToggleNotifier
import silentorb.imp.intellij.messaging.toggleTilingTopic
import silentorb.imp.intellij.ui.misc.resizeListener
import silentorb.imp.intellij.ui.preview.*
import silentorb.mythic.imaging.texturing.*
import silentorb.mythic.spatial.Vector2i
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.concurrent.locks.ReentrantLock
import javax.swing.*
import kotlin.concurrent.thread

private val gridLock = ReentrantLock()

class ImagePreviewPanel(var dimensions: Vector2i) : SimpleToolWindowPanel(true), Disposable {
  val grid = newImagePreviewChild(dimensions)
  val images: MutableMap<Vector2i, BufferedImage> = mutableMapOf()
  var startedDrawing: Boolean = false
  val connection: MessageBusConnection
  val self = this
  var state: PreviewState? = null

  init {
    val bus = ApplicationManager.getApplication().getMessageBus()
    connection = bus.connect()
    connection.subscribe(toggleTilingTopic, object : ToggleNotifier {
      override fun handle(tiling: Boolean) {
        tilingChanged(self)
      }
    })
  }

  override fun dispose() {
    connection.disconnect()
  }
}

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

fun tilingChanged(container: ImagePreviewPanel) {
  initializeImagePreviewGrid(container.grid, container.dimensions)
  val state = container.state
  if (state != null) {
    updateImagePreview(state, container)
  }
  container.grid.revalidate()
  container.grid.repaint()
}

fun initializeImagePreviewGrid(grid: JPanel, dimensions: Vector2i) {
  val repetitions = getRepetitions()
  val divisions = repetitions * cellCount
  val divisionDimensions = dimensions / divisions
  val divisionSize = Dimension(divisionDimensions.x, divisionDimensions.y)
  grid.layout = GridLayout(divisions.x, divisions.y)
  grid.removeAll()
  for (i in 0 until divisions.x * divisions.y) {
    val cell = JPanel()
    cell.preferredSize = divisionSize
    cell.background = Color.black
    grid.add(cell)
  }
}

fun newImagePreviewChild(dimensions: Vector2i): JPanel {
  val grid = JPanel()
  initializeImagePreviewGrid(grid, dimensions)
  return grid
}

fun fillImageGrid(grid: JPanel, location: Vector2i, image: BufferedImage, dimensions: Vector2i) {
  val repetitions = getRepetitions()
  val divisions = repetitions * cellCount
  val divisionDimensions = dimensions / divisions
  val scaledImage = image.getScaledInstance(divisionDimensions.x, divisionDimensions.y, Image.SCALE_DEFAULT)
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
      grid.add(newImageElement(scaledImage), index)
    }
  }
}

fun resizeImagePreview(container: ImagePreviewPanel, dimensions: Vector2i) {
  gridLock.lock()
  container.dimensions = dimensions
  val grid = container.grid
  initializeImagePreviewGrid(grid, dimensions)
  for ((location, image) in container.images) {
    fillImageGrid(grid, location, image, dimensions)
  }
  grid.revalidate()
  grid.repaint()
  gridLock.unlock()
}

fun updateImagePreview(state: PreviewState, container: ImagePreviewPanel) {
  container.state = state
  val timestamp = state.timestamp
  val executionUnit = state.executionUnit
  val type = state.type
  container.startedDrawing = true

  if (executionUnit == null)
    return

  if (isPreviewOutdated(timestamp))
    return

  val cellCoordinates = (0 until cellCount.y).flatMap { y ->
    (0 until cellCount.x).map { x ->
      Vector2i(x, y)
    }
  }
  val dimensions = container.dimensions
  val cellDimensions = dimensions / cellCount

  if (container.width == 0) {
    val j = 0
  }
  thread(start = true) {
    for (cellCoordinate in cellCoordinates) {
      if (isPreviewOutdated(timestamp) || container.dimensions != dimensions) {
//        println("$timestamp Canceled1")
        break
      }
      val value = executeToSingleValue(executionUnit)

      val sampleWriter = if (type == rgbSampler2dType.hash)
        newRgbSampleWriter(value as RgbSampler)
      else
        newFloatSampleWriter(value as FloatSampler2d)

      val image = newBufferedImage(cellDimensions, sampleWriter.depth)
      samplerToBufferedImage(sampleWriter, image, dimensions,
          cellCoordinate * cellDimensions, cellDimensions
      )

      SwingUtilities.invokeLater {
        if (!isPreviewOutdated(timestamp)) {
          gridLock.lock()
          if (container.dimensions == dimensions) {
            container.images[cellCoordinate] = image
            fillImageGrid(container.grid, cellCoordinate, image, dimensions)
            container.grid.revalidate()
            container.grid.repaint()
//            println("Updated ${cellCoordinate.x} ${cellCoordinate.y}")
          }
          gridLock.unlock()
        }
      }
    }
  }
}

fun newImagePreview(props: NewPreviewProps): PreviewDisplay {
  val dimensions = props.dimensions
  val container = ImagePreviewPanel(dimensions)
  container.addComponentListener(resizeListener(container) {
    if (container.startedDrawing) {
      resizeImagePreview(container, Vector2i(container.width))
    }
  })
  container.background = Color.black

  container.add(container.grid)
  container.setContent(container.grid)

  val actions = listOf(
      ActionManager.getInstance().getAction("silentorb.imp.intellij.actions.ToggleTilingAction")
  )
  val toolbar = newPreviewToolbar(actions)
  container.toolbar = toolbar.component

  return PreviewDisplay(
      content = container,
      toolbar = toolbar,
      update = { state ->
        updateImagePreview(state, container)
      }
  )
}
