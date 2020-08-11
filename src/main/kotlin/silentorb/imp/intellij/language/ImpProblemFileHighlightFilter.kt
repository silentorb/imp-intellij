package silentorb.imp.intellij.language

import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import silentorb.imp.intellij.misc.ImpFileType

class ImpProblemFileHighlightFilter : Condition<VirtualFile> {
  override fun value(t: VirtualFile): Boolean = t.fileType == ImpFileType.INSTANCE
}
