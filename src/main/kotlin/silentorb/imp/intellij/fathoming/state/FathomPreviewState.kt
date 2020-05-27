package silentorb.imp.intellij.fathoming.state
import java.io.Serializable

data class CameraState(
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val distance: Float = 5f
)

fun defaultCameraState() =
    CameraState(
        yaw = 0f,
        pitch = 0f,
        distance = 5f
    )

data class FathomPreviewState(
    val camera: CameraState = CameraState()
) : Serializable

fun newFathomPreviewState() =
    FathomPreviewState(
        camera = defaultCameraState()
    )
