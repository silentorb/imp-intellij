package silentorb.imp.intellij.language

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class ImpDocumentElement(debugName: String) : IElementType(debugName, ImpLanguage.INSTANCE) {
  override fun toString(): String {
    return "ImpTokenType." + super.toString()
  }
}

val impDocumentElement = ImpDocumentElement("Document")
