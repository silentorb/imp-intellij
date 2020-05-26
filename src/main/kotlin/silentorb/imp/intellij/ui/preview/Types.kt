package silentorb.imp.intellij.ui.preview

import silentorb.mythic.spatial.Vector2i
import com.intellij.openapi.editor.Document

data class NewPreviewProps(
    val dimensions: Vector2i,
    val document: Document?
)

typealias NewPreview = (NewPreviewProps) -> PreviewDisplay
