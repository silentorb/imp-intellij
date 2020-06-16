package silentorb.imp.intellij.language

import silentorb.imp.parsing.lexer.Rune

val runeTokenTypes: Map<Rune, ImpTokenType> = Rune.values().associate { Pair(it, ImpTokenType(it)) }

object ImpTokenTypes {
  val comment = runeTokenTypes.getValue(Rune.comment)
//  val definitionSymbol = runeTokenTypes.getValue(Rune.definitionSymbol)
  val identifer = runeTokenTypes.getValue(Rune.identifier)
  val keyword = runeTokenTypes.getValue(Rune.keyword)
  val literalFloat = runeTokenTypes.getValue(Rune.literalFloat)
  val literalInt = runeTokenTypes.getValue(Rune.literalInteger)
  val string = comment
  val bad = runeTokenTypes.getValue(Rune.bad)
}

val keywordStrings = listOf("let", "import")
