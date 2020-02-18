package silentorb.imp.intellij.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

object ImpSyntaxColors {
  // These are endpoints for the Settings UI so users can customize the colors.
  val SEPARATOR = TextAttributesKey.createTextAttributesKey("SIMPLE_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
  val KEYWORD = TextAttributesKey.createTextAttributesKey("SIMPLE_KEY", DefaultLanguageHighlighterColors.KEYWORD)
  val VALUE = TextAttributesKey.createTextAttributesKey("SIMPLE_VALUE", DefaultLanguageHighlighterColors.STRING)
  val COMMENT = TextAttributesKey.createTextAttributesKey("SIMPLE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val STRING = TextAttributesKey.createTextAttributesKey("STRING", DefaultLanguageHighlighterColors.STRING)
//  val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("SIMPLE_BAD_CHARACTER",
//      TextAttributes(Color.RED, null, null, null, Font.BOLD))
}
