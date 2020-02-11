package silentorb.imp.intellij.language

import com.intellij.lang.*
import com.intellij.psi.tree.IElementType
import com.jetbrains.rd.util.first
import silentorb.imp.library.standard.standardLibraryNamespace
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.parser.parseText

class ImpParser : PsiParser, LightPsiParser {
  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    val context = listOf(
        standardLibraryNamespace()
    )
    val dungeon = parseText(context)(builder.originalText)
        .throwOnFailure { Error(englishText(it.first().message)) }

    val nodeMap = dungeon.nodeMap

    while (!builder.eof()) {
      val currentTokenStart = builder.rawTokenTypeStart(builder.currentOffset)
      val node = nodeMap.entries.firstOrNull { (_, value) ->
        value.start.index == currentTokenStart
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
    return builder.treeBuilt
  }

  override fun parseLight(root: IElementType?, builder: PsiBuilder?) {

  }

}
