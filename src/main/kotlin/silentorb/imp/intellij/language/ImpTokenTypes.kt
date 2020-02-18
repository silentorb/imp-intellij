package silentorb.imp.intellij.language

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import silentorb.imp.parsing.lexer.Rune

val runeTokenTypes: Map<Rune, ImpTokenType> = Rune.values().associate { Pair(it, ImpTokenType(it)) }

object ImpTokenTypes {
  val comment = runeTokenTypes[Rune.comment]!!
  val string = comment
}


fun nodeToElement(node: ASTNode): PsiElement {
  return ImpPsiElement(node)
}
