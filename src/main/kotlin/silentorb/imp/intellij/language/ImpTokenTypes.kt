package silentorb.imp.intellij.language

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import silentorb.imp.parsing.lexer.Rune

object ImpTokenTypes {
  val Comment = ImpTokenType("comment")
}

val runeTokenTypes: Map<Rune, ImpTokenType> = Rune.values().associate { Pair(it, ImpTokenType(it.name)) }

fun nodeToElement(node: ASTNode): PsiElement {
  throw AssertionError("Not yet implemented")
}
