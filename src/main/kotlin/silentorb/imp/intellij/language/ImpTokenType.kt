package silentorb.imp.intellij.language

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import silentorb.imp.parsing.lexer.Rune

class ImpTokenType(val rune: Rune) : IElementType(rune.name, ImpLanguage.INSTANCE) {
  override fun toString(): String {
    return "ImpTokenType." + super.toString()
  }
}
