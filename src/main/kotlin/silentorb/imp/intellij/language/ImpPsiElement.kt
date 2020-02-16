package silentorb.imp.intellij.language

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class ImpPsiElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiElement {

}
