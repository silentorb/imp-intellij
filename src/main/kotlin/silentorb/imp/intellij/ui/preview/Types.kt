package silentorb.imp.intellij.ui.preview

import com.intellij.openapi.actionSystem.ActionToolbar
import silentorb.mythic.spatial.Vector2i
import com.intellij.openapi.editor.Document
import silentorb.imp.core.Dungeon
import silentorb.imp.core.PathKey
import silentorb.imp.core.TypeHash
import silentorb.imp.execution.ExecutionStep
import silentorb.imp.execution.ExecutionUnit
import javax.swing.JComponent

data class NewPreviewProps(
    val dimensions: Vector2i,
    val document: Document?
)

typealias NewPreview = (NewPreviewProps) -> PreviewDisplay

data class PreviewDisplay(
    val content: JComponent,
    val toolbar: ActionToolbar? = null,
    val update: (PreviewState) -> Unit
)

data class PreviewState(
    val document: Document?,
    val dungeon: Dungeon,
    val node: PathKey?,
    val executionUnit: ExecutionUnit?,
    val type: TypeHash,
    val timestamp: Long
)
