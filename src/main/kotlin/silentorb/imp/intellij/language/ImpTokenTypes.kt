package silentorb.imp.intellij.language

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.lexer.Rune

object ImpTokenTypes {
  val Comment = ImpTokenType(Rune.comment)
  val String = ImpTokenType(Rune.comment)
}

val runeTokenTypes: Map<Rune, ImpTokenType> = Rune.values().associate { Pair(it, ImpTokenType(it)) }

fun nodeToElement(node: ASTNode): PsiElement {
  return ImpPsiElement(node)
}
