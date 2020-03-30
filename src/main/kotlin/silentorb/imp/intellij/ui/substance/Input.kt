package silentorb.imp.intellij.ui.substance

import silentorb.mythic.spatial.Pi
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.lang.Float.max
import javax.swing.JComponent
import javax.swing.event.MouseInputAdapter

fun radial(value: Float): Float =
    (value +  Pi * 2f) % (Pi * 2f)

fun initializeCameraUi(component: JComponent, getState: () -> CameraState, setState: (CameraState) -> Unit) {
  val mouseListener = object : MouseInputAdapter() {
    var lastX: Int? = null
    var lastY: Int? = null

    override fun mouseDragged(event: MouseEvent?) {
      super.mouseDragged(event)
      if (event != null) {
//        println("dragged ${event.x} ${event.y}")
        val localLastX = lastX
        if (localLastX != null) {
          val offsetX = event.x - localLastX
          val state = getState()
          setState(state.copy(
              yaw = radial(state.yaw - offsetX.toFloat() * 0.05f)
          ))
        }
        lastX = event.x
        lastY = event.y
      }
    }

    override fun mouseMoved(event: MouseEvent?) {
      super.mouseMoved(event)
      if (event != null) {
        lastX = event.x
        lastY = event.y
//        println("moved ${event.x} ${event.y}")
      }
    }

    override fun mouseWheelMoved(event: MouseWheelEvent?) {
      super.mouseWheelMoved(event)
      if (event != null) {
        val offset = event.wheelRotation
        val state = getState()
        setState(state.copy(
            distance = max(1f, state.distance + offset.toFloat() * 0.2f)
        ))
      }
    }
  }

  component.addMouseListener(mouseListener)
  component.addMouseMotionListener(mouseListener)
  component.addMouseWheelListener(mouseListener)
}
