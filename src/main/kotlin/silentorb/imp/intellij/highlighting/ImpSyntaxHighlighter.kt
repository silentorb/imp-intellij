package silentorb.imp.intellij.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import silentorb.imp.intellij.language.ImpLexer
import silentorb.imp.intellij.language.ImpTokenTypes

class ImpSyntaxHighlighter : SyntaxHighlighterBase() {
  companion object {
    val comment = createTextAttributesKey("comment", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val bad = createTextAttributesKey("bad", HighlighterColors.BAD_CHARACTER)
    val keyword = createTextAttributesKey("keyword", DefaultLanguageHighlighterColors.KEYWORD)
    val number = createTextAttributesKey("number", DefaultLanguageHighlighterColors.NUMBER)
    val previewNode = createTextAttributesKey("previewNode", DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_HIGHLIGHTED)

    val commentKeys = arrayOf(comment)
    val keywordKeys = arrayOf(keyword)
    val emptyKeys = arrayOf<TextAttributesKey>()
    val badTokenKeys = arrayOf(bad)
    val numberKeys = arrayOf(number)
    val previewNodeKeys = arrayOf(previewNode)
  }

  override fun getHighlightingLexer(): Lexer {
    return ImpLexer()
  }

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
      when (tokenType) {
        ImpTokenTypes.comment -> commentKeys
        ImpTokenTypes.keyword -> keywordKeys
        ImpTokenTypes.bad -> badTokenKeys
        ImpTokenTypes.literalFloat -> numberKeys
        ImpTokenTypes.literalInt -> numberKeys
        else -> emptyKeys
      }

}
