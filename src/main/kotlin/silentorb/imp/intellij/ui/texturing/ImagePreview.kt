package silentorb.imp.intellij.ui.texturing

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.messages.MessageBusConnection
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.OutputValues
import silentorb.imp.execution.executeStep
import silentorb.imp.intellij.services.initialFunctions
import silentorb.imp.intellij.messaging.ToggleTilingNotifier
import silentorb.imp.intellij.messaging.toggleTilingTopic
import silentorb.imp.intellij.ui.preview.NewPreviewProps
import silentorb.imp.intellij.ui.preview.PreviewDisplay
import silentorb.imp.intellij.ui.preview.PreviewState
import silentorb.imp.intellij.ui.preview.isPreviewOutdated
import silentorb.mythic.imaging.texturing.*
import silentorb.mythic.spatial.Vector2i
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Image
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.image.BufferedImage
import java.util.concurrent.locks.ReentrantLock
import javax.swing.*
import kotlin.concurrent.thread


private val gridLock = ReentrantLock()

class ImagePreviewPanel(var dimensions: Vector2i) : JPanel(), Disposable {
  val grid = newImagePreviewChild(dimensions)
  val images: MutableMap<Vector2i, BufferedImage> = mutableMapOf()
  var startedDrawing: Boolean = false
  val connection: MessageBusConnection
  val self = this
  var state: PreviewState? = null

  init {
    val bus = ApplicationManager.getApplication().getMessageBus()
    connection = bus.connect()
    connection.subscribe(toggleTilingTopic, object : ToggleTilingNotifier {
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
  val graph = state.graph
  val steps = state.steps
  val type = state.type
  container.startedDrawing = true

  if (isPreviewOutdated(timestamp))
    return

  val cellCoordinates = (0 until cellCount.y).flatMap { y ->
    (0 until cellCount.x).map { x ->
      Vector2i(x, y)
    }
  }
  val dimensions = container.dimensions
  val cellDimensions = dimensions / cellCount
  val functions = initialFunctions()

  if (container.width == 0) {
    val j = 0
  }
  thread(start = true) {
    for (cellCoordinate in cellCoordinates) {
      if (isPreviewOutdated(timestamp) || container.dimensions != dimensions) {
//        println("$timestamp Canceled1")
        break
      }
      val additionalArguments = mapOf("__globalDimensions" to dimensions)
      var values: OutputValues = mapOf()
      val output = state.node ?: getGraphOutputNode(graph)
      for (step in steps) {
        values = executeStep(functions, graph, values, step, additionalArguments)
        if (step == output)
          break
      }
      val value = values[output]
      if (value == null)
        break

      val sampleWriter = if (type == rgbSampler2dKey)
        newRgbSampleWriter(value as RgbSampler)
      else
        newFloatSampleWriter(value as FloatSampler)

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

fun resizeListener(container: ImagePreviewPanel) =
    object : ComponentListener {
      var previousWidth = container.width
      override fun componentResized(event: ComponentEvent?) {
        if (container.startedDrawing && container.width != previousWidth) {
//          println("Changed width $previousWidth -> ${container.width}")
          previousWidth = container.width
          resizeImagePreview(container, Vector2i(container.width))
        }
      }

      override fun componentMoved(e: ComponentEvent?) {}
      override fun componentHidden(e: ComponentEvent?) {}
      override fun componentShown(e: ComponentEvent?) {}
    }

fun newImagePreview(props: NewPreviewProps): PreviewDisplay {
  val dimensions = props.dimensions
//  println("new ImagePreview ${dimensions.x} ${dimensions.y}")
  val container = ImagePreviewPanel(dimensions)
//  container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
  container.addComponentListener(resizeListener(container))
  container.background = Color.black

//  val gridWrapper = JPanel()
  container.add(container.grid)

  val actionManager = ActionManager.getInstance()
  val actionGroup = DefaultActionGroup("ACTION_GROUP", false)
  actionGroup.add(ActionManager.getInstance().getAction("silentorb.imp.intellij.actions.ToggleTilingAction"))
  val actionToolbar = actionManager.createActionToolbar("ACTION_GROUP", actionGroup, true)
  actionToolbar.component.preferredSize = Dimension(0, 40)
  return PreviewDisplay(
      content = container,
      toolbar = actionToolbar,
      update = { state ->
        updateImagePreview(state, container)
      }
  )
}