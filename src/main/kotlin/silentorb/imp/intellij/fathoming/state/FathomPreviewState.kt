package silentorb.imp.intellij.fathoming.state
import java.io.Serializable

data class CameraState(
    val yaw: Float,
    val pitch: Float,
    val distance: Float
)

fun defaultCameraState() =
    CameraState(
        yaw = 0f,
        pitch = 0f,
        distance = 5f
    )

data class FathomPreviewState(
    val camera: CameraState
) : Serializable

fun newFathomPreviewState() =
    FathomPreviewState(
        camera = defaultCameraState()
    )
