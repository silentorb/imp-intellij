package silentorb.imp.intellij.ui.preview

import silentorb.mythic.spatial.Vector2i

data class NewPreviewProps(
    val dimensions: Vector2i
)

typealias NewPreview = (NewPreviewProps) -> PreviewDisplay
