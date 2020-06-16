package silentorb.imp.intellij.language

import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.parser.tokenizeAndSanitize
import silentorb.imp.parsing.syntax.toTokenGraph
import java.nio.file.Paths

class ImpParser : PsiParser, LightPsiParser {
  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    val filePath = Paths.get("")
    val (tokens, lexingErrors) = tokenizeAndSanitize(filePath.toString(), builder.originalText)
    val (tokenizedGraph, tokenGraphErrors) = toTokenGraph(filePath.toString(), tokens)
    val errors = lexingErrors + tokenGraphErrors

    val ignoreErrors = runeTokenTypes.containsValue(root)

    val documentMarker = builder.mark()
    while (!builder.eof()) {
      val currentTokenStart = builder.currentOffset
      if (!ignoreErrors) {
        val error = errors.firstOrNull { it.fileRange.range.start.index == currentTokenStart }
        if (error != null) {
          builder.error(englishText(error.message))
        }
      }
      val definitionSymbol = tokenizedGraph.definitions
          .map { it.symbol }
          .firstOrNull { it.range.start.index == currentTokenStart }

      val tokenType =
//        if (definitionSymbol != null)
//        ImpTokenTypes.definitionSymbol
//      else
        builder.tokenType!!

      val marker = builder.mark()
      builder.advanceLexer()
      marker.done(tokenType)
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
