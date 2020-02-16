package silentorb.imp.intellij.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.relaxNG.compact.RncTokenTypes.KEYWORDS
import silentorb.imp.intellij.highlighting.ImpSyntaxColors.BAD_CHARACTER
import silentorb.imp.intellij.language.ImpLexer
import silentorb.imp.intellij.language.ImpTokenTypes
import java.util.*

class ImpSyntaxHighlighter : SyntaxHighlighterBase() {
  companion object {
    private val ATTRIBUTES: MutableMap<IElementType, TextAttributesKey?> = HashMap()

    init {
      fillMap(ATTRIBUTES, KEYWORDS, ImpSyntaxColors.KEYWORD)
      ATTRIBUTES[TokenType.BAD_CHARACTER] = BAD_CHARACTER
      ATTRIBUTES[ImpTokenTypes.String] = ImpSyntaxColors.STRING
    }
  }

  override fun getHighlightingLexer(): Lexer {
    return ImpLexer()
  }

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
    return pack(ATTRIBUTES[tokenType])
  }
}
