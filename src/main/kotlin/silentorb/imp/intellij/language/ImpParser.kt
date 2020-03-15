package silentorb.imp.intellij.language

import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.general.Response
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.parser.parseText
import silentorb.imp.parsing.parser.parseTextBranching

class ImpParser : PsiParser, LightPsiParser {
  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    val context = initialContext()
    val (dungeon, errors) = parseText(context)(builder.originalText)

    val nodeMap = dungeon.nodeMap

    val ignoreErrors = runeTokenTypes.containsValue(root)

    val documentMarker = builder.mark()
    while (!builder.eof()) {
      val currentTokenStart = builder.currentOffset
      val node = nodeMap.entries.firstOrNull { (_, value) ->
        value.start.index == currentTokenStart
      }
      if (!ignoreErrors) {
        val error = errors.firstOrNull { it.range.start.index == currentTokenStart }
        if (error != null) {
          builder.error(englishText(error.message))
        }
      }
      val tokenType = builder.tokenType!!
      val marker = builder.mark()
      builder.advanceLexer()
      if (node != null) {
        marker.done(tokenType)
      } else {
        marker.done(tokenType)
      }
    }
    documentMarker.done(impDocumentElement)
    return builder.treeBuilt
  }

  override fun parseLight(root: IElementType?, builder: PsiBuilder?) {
    if (root != null && builder != null) {
      parse(root, builder)
    }
  }

}
