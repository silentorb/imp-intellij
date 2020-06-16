package silentorb.imp.intellij.language

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class ImpPsiElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiElement {

}

class ImpPsiElementIdentifier(node: ASTNode) : ASTWrapperPsiElement(node), PsiElement {
  override fun getReference(): PsiReference? {
    return super.getReference()
  }
}

fun nodeToElement(node: ASTNode): PsiElement =
    when (node.elementType) {
      ImpTokenTypes.identifer -> ImpPsiElementIdentifier(node)
      else -> ImpPsiElement(node)
    }
