package silentorb.imp.intellij.ui.controls

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.ui.content.ContentManager
import silentorb.imp.campaign.getModulesContext
import silentorb.imp.core.*
import silentorb.imp.core.Range
import silentorb.imp.core.Dungeon
import silentorb.imp.intellij.services.getWorkspaceArtifact
import silentorb.imp.intellij.ui.misc.*
import silentorb.mythic.imaging.texturing.newRgbTypeHash
import java.awt.Dimension
import java.nio.file.Paths
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
        val caretOffset = getCurrentEditorCaretOffset(project, file)
        lastCaretOffset = caretOffset
        if (caretOffset != null) {
          val document = FileDocumentManager.getInstance().getDocument(file)!!
          val response = getDungeonAndErrors(project, document)
          if (response != null) {
            val (dungeon, errors) = response
            if (errors.none()) {
              val workspaceResponse = getWorkspaceArtifact(Paths.get(file.path))
              val context = if (workspaceResponse != null)
                getModulesContext(workspaceResponse.value.modules)
              else
                listOf(dungeon.namespace)

              updateControlPanel(
                getPsiElement(project, document),
                changePsiValue(project),
                context,
                this,
                dungeon,
                file.path,
                caretOffset,
                tracker
              )
            }
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
  newRgbTypeHash to ::newColorPicker
)

fun newFieldControl(
  getPsiElement: GetPsiValue,
  context: Context,
  dungeon: Dungeon,
  node: PathKey,
  type: TypeHash?
): ControlField? {
  val graph = dungeon.namespace
  val complexTypeControl = complexTypeControls[type]
  val parameters = getParameterConnections(context, node)

  return if (type != null && parameters.any()) {
    val nodes = if (complexTypeControl != null) {
      dungeon.namespace.connections
        .filter { it.key.destination == node }
        .map { it.value }
    } else
      listOf(node)

    val offsets = dungeon.namespace.connections
      .filter { it.key.destination == node }
      .map { it.value }
      .plus(node)
      .mapNotNull { dungeon.nodeMap[it]?.range?.start?.index }

    val psiElements = offsets.map(getPsiElement).filterNotNull()
    if (psiElements.any())
      ControlField(
        name = parameters.keys.first().parameter,
        nodes = nodes,
        psiElements = psiElements,
        type = type,
        valueRange = resolveNumericTypeConstraint(type)(context)
      )
    else
      null
  } else
    null
}

fun gatherControlFields(
  getPsiElement: GetPsiValue,
  context: Context,
  dungeon: Dungeon,
  node: PathKey
): List<ControlField> {
  val graph = dungeon.namespace
  val functionType = graph.nodeTypes[node]
  return if (functionType != null && !complexTypeControls.containsKey(functionType)) {

    val connections = getArgumentConnections(context, node)
    connections.mapNotNull { connection ->
      val child = connection.value
      newFieldControl(
        getPsiElement,
        context,
        dungeon,
        connection.value,
        graph.nodeTypes[child]
      )
    }
  } else {
    listOfNotNull(newFieldControl(getPsiElement, context, dungeon, node, functionType))
  }
}

fun updateControlList(changePsiValue: ChangePsiValue, values: Map<PathKey, Any>, field: ControlField): JComponent? {
  val nodes = field.nodes
  val psiElements = field.psiElements

  val row = JPanel()
  val constraintRange = field.valueRange
  if (constraintRange != null) {
    val value = values[nodes.first()]!!
    newSlider(changePsiValue, psiElements.first(), value as Int, constraintRange, row)
  } else if (complexTypeControls.containsKey(field.type)) {
    val childValues = nodes.mapNotNull { values[it] }
    row.add(complexTypeControls[field.type]!!(changePsiValue, psiElements, childValues))
  } else {
    return null
  }
  val label = JLabel(field.name, SwingConstants.RIGHT)
//  label.setPreferredSize(Dimension(80, 30))
  row.add(label)
  return row
}

fun updateControlPanel(
  getPsiElement: GetPsiValue, changePsiValue: ChangePsiValue, context: Context, controls: JPanel,
  dungeon: Dungeon, file: String, offset: Int, tracker: ControlTracker?
): ControlTracker? {
  val nodeRange = findNodeEntry(dungeon.nodeMap, file, offset)
  val node = nodeRange?.key

  return if (node != null) {
    val newTracker = ControlTracker(
      node = node,
      range = nodeRange.value.range
    )
    if (tracker != newTracker) {
      controls.removeAll()
      val fields = gatherControlFields(getPsiElement, context, dungeon, node)
      fields.forEach { field ->
        val fieldComponent = updateControlList(changePsiValue, dungeon.namespace.values, field)
        if (fieldComponent != null)
          controls.add(fieldComponent)
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
