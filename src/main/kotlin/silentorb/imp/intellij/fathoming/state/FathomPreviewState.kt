package silentorb.imp.intellij.fathoming.state

data class CameraState(
    var yaw: Float = 0f,
    var pitch: Float = 0f,
    var distance: Float = 5f
)

fun defaultCameraState() =
    CameraState(
        yaw = 0f,
        pitch = 0f,
        distance = 5f
    )

data class FathomPreviewState(
    var camera: CameraState = CameraState()
)

fun newFathomPreviewState() =
    FathomPreviewState(
        camera = defaultCameraState()
    )
