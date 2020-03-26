package silentorb.imp.intellij.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import silentorb.imp.intellij.highlighting.ImpSyntaxHighlighter
import silentorb.imp.intellij.services.getDocumentMetadataService
import silentorb.imp.intellij.ui.findNode
import silentorb.imp.intellij.ui.getDungeonAndErrors
import silentorb.imp.parsing.lexer.Rune

class ImpAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val type = element.elementType
    if (type is ImpTokenType && type.rune === Rune.identifier) {
      val response = getDungeonAndErrors(element.project, element.containingFile)
      if (response != null) {
        val (dungeon, errors) = response
        val node = findNode(dungeon.nodeMap, element.textOffset)
        if (node != null) {
          val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
          if (document != null) {
            if (node == getDocumentMetadataService().getPreviewNode(document)) {
              val annotation = holder.createInfoAnnotation(element, "Preview")
              annotation.textAttributes = ImpSyntaxHighlighter.previewNode
            }
          }
        }
      }
    }
  }
}
