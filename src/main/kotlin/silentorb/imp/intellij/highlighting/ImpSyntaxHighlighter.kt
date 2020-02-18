package silentorb.imp.intellij.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import silentorb.imp.intellij.language.ImpLexer
import silentorb.imp.intellij.language.ImpTokenTypes

class ImpSyntaxHighlighter : SyntaxHighlighterBase() {
  companion object {
    val comment = createTextAttributesKey("comment", DefaultLanguageHighlighterColors.LINE_COMMENT)

    val commentKeys = arrayOf(comment)
    val emptyKeys = arrayOf<TextAttributesKey>()
  }

  override fun getHighlightingLexer(): Lexer {
    return ImpLexer()
  }

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
      when (tokenType) {
        ImpTokenTypes.comment -> commentKeys
        else -> emptyKeys
      }

}
