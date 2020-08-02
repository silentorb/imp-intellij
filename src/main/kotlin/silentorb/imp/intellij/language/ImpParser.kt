package silentorb.imp.intellij.language

import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.parser.tokenizeAndSanitize
import silentorb.imp.parsing.syntax.BurgId
import silentorb.imp.parsing.syntax.parseSyntax
import java.nio.file.Paths

class ImpParser : PsiParser, LightPsiParser {
  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    if (root is ImpTokenType) {
      val marker = builder.mark()
      while (!builder.eof()) {
        builder.advanceLexer()
      }
      marker.done(root)
      return builder.treeBuilt
    }

    val filePath = Paths.get("")
    val (tokens, lexingErrors) = tokenizeAndSanitize(filePath.toString(), builder.originalText)
    val (realm, syntaxErrors) = parseSyntax("", tokens)
    val burgs = realm.burgs.values
    val errors = (lexingErrors + syntaxErrors).filter { it.fileRange != null }

    val documentMarker = builder.mark()
    var markerStack = listOf<Pair<PsiBuilder.Marker, IElementType>>()
    while (!builder.eof()) {
      val currentTokenStart = builder.currentOffset
      errors.filter { it.fileRange!!.range.start.index == currentTokenStart }
          .forEach { error ->
            builder.error(englishText(error.message))
          }
      val startBurgs = burgs
          .filter { it.range.start.index == currentTokenStart }
          .sortedByDescending { it.range.end.index }

      val endBurgs = burgs
          .filter { it.range.end.index == currentTokenStart }

      if (endBurgs.size > markerStack.size) {
        println("Error: markerStack not big enough")
      }

      markerStack.takeLast(endBurgs.size)
          .reversed()
          .forEach { (marker, tokenType) ->
            marker.done(tokenType)
          }

      markerStack = markerStack
          .dropLast(endBurgs.size)

      markerStack = markerStack
          .plus(startBurgs.map {
            builder.mark() to nodeTypes[it.type]!!
          })

      builder.advanceLexer()
    }
    if (markerStack.any()) {
      println("Error: markerStack not empty")
    }
    markerStack
        .reversed()
        .forEach { (marker, tokenType) ->
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
