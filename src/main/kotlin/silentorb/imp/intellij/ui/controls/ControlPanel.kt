package silentorb.imp.intellij.ui.controls

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.ui.content.ContentManager
import silentorb.imp.core.*
import silentorb.imp.intellij.services.initialContext
import silentorb.imp.intellij.ui.misc.ActiveDocumentWatcher
import silentorb.imp.intellij.ui.misc.changePsiValue
import silentorb.imp.intellij.ui.misc.findNodeEntry
import silentorb.imp.intellij.ui.misc.getPsiElement
import silentorb.imp.parsing.general.Range
import silentorb.imp.parsing.parser.Dungeon
import silentorb.imp.parsing.parser.parseText
import silentorb.mythic.imaging.texturing.rgbColorType
import java.awt.Dimension
import javax.swing.*

fun getCurrentEditorCaretOffset(project: Project, file: VirtualFile): Int? {
  val editor = FileEditorManager.getInstance(project).getSelectedEditor(file)
  return if (editor != null) {
    (editor as TextEditorImpl).editor.caretModel.offset
  } else
    null
}

class ControlPanel(val project: Project, contentManager: ContentManager) : JPanel() {
  var tracker: ControlTracker? = null
  var lastCaretOffset: Int? = null
  var lastFile: VirtualFile? = null
  val activeDocumentWatcher = ActiveDocumentWatcher(project) { file ->
    if (file !== lastFile || (file != null && getCurrentEditorCaretOffset(project, file) != lastCaretOffset)) {
      lastFile = file
      if (file == null) {
        clearControlList(this)
      } else {
        // Todo: Somehow get shared/cached dungeon from ImpParser
        val caretOffset = getCurrentEditorCaretOffset(project, file)
        lastCaretOffset = caretOffset
        if (caretOffset != null) {
          val document = FileDocumentManager.getInstance().getDocument(file)!!
          val context = initialContext()
          val (dungeon, errors) = parseText(context)(document.text)
          if (errors.none()) {
            updateControlPanel(getPsiElement(project, document), changePsiValue(project), context, this, dungeon, caretOffset, tracker)
          }
        }
      }
    }
  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    activeDocumentWatcher.start(contentManager)
  }
}

data class ControlTracker(
    val node: PathKey,
    val range: Range
)

fun clearControlList(controls: JPanel) {
  controls.removeAll()
  controls.revalidate()
  controls.repaint()
}

data class PsiElementWrapper(
    var element: PsiElement
)

typealias GetPsiValue = (Int) -> PsiElementWrapper?
typealias ChangePsiValue = (PsiElementWrapper, String) -> Unit

data class ControlField(
    val name: String,
    val nodes: List<PathKey>,
    val psiElements: List<PsiElementWrapper>,
    val type: TypeHash,
    val valueRange: NumericTypeConstraint?
)

typealias ComplexTypeControl = (ChangePsiValue, List<PsiElementWrapper>, List<Any>) -> JComponent

private val complexTypeControls: Map<TypeHash, ComplexTypeControl> = mapOf(
    rgbColorType to ::newColorPicker
)
    .mapKeys { it.key.hash }

fun newFieldControl(getPsiElement: GetPsiValue, context: Context, dungeon: Dungeon, node: PathKey): ControlField? {
  val graph = dungeon.graph
  val type = graph.nodeTypes[node]
  val complexTypeControl = complexTypeControls[type]
//  val type = graph.implementationTypes[node]
//      ?: if (complexTypeControl != null)
//        functionType
//      else
//        null

  val connections = graph.connections.filter { it.source == node }
  val parameters = connections
//      .flatMap { match ->
//    match.alignment.filter { it.value == node }.keys
//  }
  return if (type != null && parameters.any()) {
    val nodes = if (complexTypeControl != null) {
      dungeon.graph.connections
          .filter { it.destination == node }
          .map { it.source }
    } else
      listOf(node)

    val offsets = dungeon.graph.connections
        .filter { it.destination == node }
        .map { it.source }
        .plus(node)
        .map { dungeon.nodeMap[it]!!.start.index }

    val psiElements = offsets.map(getPsiElement).filterNotNull()
    if (psiElements.any())
      ControlField(
          name = parameters.first().parameter,
          nodes = nodes,
          psiElements = psiElements,
          type = type,
          valueRange = resolveNumericTypeConstraint(context, type)
      )
    else
      null
  } else
    null
}

fun gatherControlFields(getPsiElement: GetPsiValue, context: Context, dungeon: Dungeon, node: PathKey): List<ControlField> {
  val graph = dungeon.graph
  val function = graph.implementationTypes[node]
  return if (function != null && !complexTypeControls.containsKey(function)) {

    val connections = graph.connections.filter { it.destination == node }
    connections.mapNotNull { connection ->
      newFieldControl(getPsiElement, context, dungeon, connection.source)
    }
  } else {
    listOfNotNull(newFieldControl(getPsiElement, context, dungeon, node))
  }
}

fun updateControlList(changePsiValue: ChangePsiValue, values: Map<PathKey, Any>, field: ControlField): JComponent {
  val nodes = field.nodes
  val psiElements = field.psiElements

  val row = JPanel()
  val constraintRange = field.valueRange
  if (constraintRange != null) {
    val value = values[nodes.first()]!!
    newSlider(changePsiValue, psiElements.first(), value as Int, constraintRange, row)
  } else if (complexTypeControls.containsKey(field.type)) {
    val childValues = nodes.map { values[it]!! }
    row.add(complexTypeControls[field.type]!!(changePsiValue, psiElements, childValues))
  }
  val label = JLabel(field.name, SwingConstants.RIGHT)
  label.setPreferredSize(Dimension(80, 30))
  row.add(label)
  return row
}

fun updateControlPanel(getPsiElement: GetPsiValue, changePsiValue: ChangePsiValue, context: Context, controls: JPanel, dungeon: Dungeon, offset: Int, tracker: ControlTracker?): ControlTracker? {
  val nodeRange = findNodeEntry(dungeon.nodeMap, offset)
  val node = nodeRange?.key

  return if (node != null) {
    val newTracker = ControlTracker(
        node = node,
        range = nodeRange.value
    )
    if (tracker != newTracker) {
      controls.removeAll()
      val fields = gatherControlFields(getPsiElement, context, dungeon, node)
      fields.forEach { field ->
        controls.add(updateControlList(changePsiValue, dungeon.graph.values, field))
      }
      controls.revalidate()
      controls.repaint()
    }
    newTracker
  } else {
    if (controls.componentCount > 0)
      clearControlList(controls)
    null
  }
}
