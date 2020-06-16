package silentorb.imp.intellij.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import silentorb.imp.core.Dungeon
import silentorb.imp.intellij.highlighting.ImpSyntaxHighlighter
import silentorb.imp.intellij.services.getDocumentMetadataService
import silentorb.imp.intellij.ui.misc.findNode
import silentorb.imp.intellij.ui.misc.getDungeonAndErrors
import silentorb.imp.parsing.lexer.Rune

fun previewSelectionAnnotations(dungeon: Dungeon, document: Document, element: PsiElement, holder: AnnotationHolder) {
  val node = findNode(dungeon.nodeMap, element.textOffset)
  if (node != null) {
    if (node == getDocumentMetadataService().getPreviewNode(document)) {
      val annotation = holder.createInfoAnnotation(element, "Preview")
      annotation.textAttributes = ImpSyntaxHighlighter.previewNode
    }
  }
}

class ImpAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val type = element.elementType
    if (type is ImpTokenType && type.rune === Rune.identifier) {
      val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
      if (document != null) {
        val response = getDungeonAndErrors(element.project, element.containingFile)
        if (response != null) {
          val (dungeon, errors) = response
          previewSelectionAnnotations(dungeon, document, element, holder)
        }
      }
    }
  }
}
