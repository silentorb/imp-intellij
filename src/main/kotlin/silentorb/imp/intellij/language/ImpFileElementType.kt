package silentorb.imp.intellij.language

import com.intellij.psi.tree.IFileElementType

class ImpFileElementType() : IFileElementType(ImpLanguage.INSTANCE) {
  companion object {
    val INSTANCE = ImpFileElementType()
  }
}
