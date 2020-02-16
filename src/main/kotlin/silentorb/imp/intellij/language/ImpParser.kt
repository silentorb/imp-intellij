package silentorb.imp.intellij.language

import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.general.Response
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.parser.parseText

class ImpParser : PsiParser, LightPsiParser {
  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    val context = initialContext()
    val parseResult = parseText(context)(builder.originalText)

    val dungeon = if (parseResult is Response.Success)
      parseResult.value
    else
      null

    val nodeMap = dungeon?.nodeMap
    val errors = if (parseResult is Response.Failure)
      parseResult.errors
    else
      listOf()

    val documentMarker = builder.mark()
    while (!builder.eof()) {
      val currentTokenStart = builder.currentOffset
      val node = nodeMap?.entries?.firstOrNull { (_, value) ->
        value.start.index == currentTokenStart
      }
      val error = errors.firstOrNull { it.range.start.index == currentTokenStart}
      if (error != null) {
        builder.error(englishText(error.message))
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

  }

}
