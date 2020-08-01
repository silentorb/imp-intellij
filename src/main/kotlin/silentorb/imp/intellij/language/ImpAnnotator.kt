package silentorb.imp.intellij.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
//import com.intellij.refactoring.suggested.endOffset
//import com.intellij.refactoring.suggested.startOffset
import silentorb.imp.core.FileRange
import silentorb.imp.core.ImpErrors
import silentorb.imp.core.PathKey
import silentorb.imp.core.formatErrorMessage
import silentorb.imp.intellij.highlighting.ImpSyntaxHighlighter
import silentorb.imp.intellij.services.getDocumentFile
import silentorb.imp.intellij.services.getDocumentMetadataService
import silentorb.imp.intellij.ui.misc.findNodeEntry
import silentorb.imp.intellij.ui.misc.getDungeonAndErrors
import silentorb.imp.parsing.general.englishText
import silentorb.imp.parsing.lexer.Rune

fun previewSelectionAnnotations(
  node: PathKey,
  filePath: String,
  element: PsiElement,
  holder: AnnotationHolder
) {
  if (node == getDocumentMetadataService().getPreviewNode(filePath)) {
    val annotation = holder.createInfoAnnotation(element, "Preview")
    annotation.textAttributes = ImpSyntaxHighlighter.previewNode
//    holder.newAnnotation(HighlightSeverity.INFORMATION, "Preview")
//      .range(element)
//      .textAttributes(ImpSyntaxHighlighter.previewNode)
//      .create()
  }
}

fun rangeMatchesElement(fileRange: FileRange, element: PsiElement, filePath: String): Boolean =
//  fileRange.range.start.index == element.startOffset &&
  fileRange.range.start.index == element.textOffset &&
      fileRange.file == filePath

fun annotateErrors(
  holder: AnnotationHolder,
  errors: ImpErrors,
  filePath: String,
  element: PsiElement
) {
  errors.mapNotNull { error ->
    val fileRange = error.fileRange
    if (fileRange != null && rangeMatchesElement(fileRange, element, filePath)) {
      holder.createErrorAnnotation(element, formatErrorMessage(::englishText, error))
//      holder.newAnnotation(HighlightSeverity.ERROR, formatErrorMessage(::englishText, error))
//        .range(element)
//        .create()
    } else
      null
  }
}

class ImpAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val type = element.elementType
    if (type is ImpTokenType) {
      val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
      if (document != null) {
        val response = getDungeonAndErrors(element.project, element.containingFile)
        if (response != null) {
          val (dungeon, errors) = response
          val filePath = getDocumentFile(document)?.canonicalPath!!
          val node = findNodeEntry(dungeon.nodeMap, filePath, element.textOffset)
          if (node != null) {
            previewSelectionAnnotations(node.key, filePath, element, holder)
          }
          annotateErrors(holder, errors, filePath, element)
        }
      }
    }
  }
}
