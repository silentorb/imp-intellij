package silentorb.imp.intellij.ui.controls

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.ui.content.ContentManager
import silentorb.imp.campaign.getModulesContext
import silentorb.imp.core.*
import silentorb.imp.intellij.services.getWorkspaceArtifact
import silentorb.imp.intellij.services.initialContext
import silentorb.imp.intellij.ui.misc.*
import silentorb.mythic.imaging.texturing.newRgbTypeHash
import java.nio.file.Paths
import javax.swing.*

fun getCurrentEditorCaretOffset(project: Project, file: VirtualFile): Int? {
  val editor = FileEditorManager.getInstance(project).getSelectedEditor(file)
  return if (editor != null) {
    (editor as TextEditorImpl).editor.caretModel.offset
  } else
    null
}

class ControlPanel(val project: Project, contentManager: ContentManager) : JPanel(), Disposable {
  var tracker: ControlTracker? = null
  var lastCaretOffset: Int? = null
  var lastFile: VirtualFile? = null

  fun refreshControls(document: Document, filePath: String, caretOffset: Int) {
    val response = getDungeonAndErrors(project, document)
    if (response != null) {
      val (dungeon, errors) = response
      if (errors.none()) {
        val workspaceResponse = getWorkspaceArtifact(Paths.get(filePath))
        val context = if (workspaceResponse != null)
          initialContext() + getModulesContext(workspaceResponse.value.modules)
        else
          listOf(dungeon.namespace)

        try {
          updateControlPanel(
              getPsiElement(project, document),
              changePsiValue(project),
              context,
              this,
              dungeon,
              filePath,
              caretOffset,
              tracker
          )
        } catch (error: Throwable) {
          // This is just for emergencies
        }
      }
    }
  }

  fun onTick() {
    val file = getActiveVirtualFile(project)
    val document = if (file != null) FileDocumentManager.getInstance().getDocument(file) else null
    if (file == null && lastFile != null) {
      clearControlList(this)
    } else if (file != null && document != null && (file !== lastFile || getCurrentEditorCaretOffset(project, file) != lastCaretOffset)) {
      lastFile = file
      val caretOffset = getCurrentEditorCaretOffset(project, file)
      lastCaretOffset = caretOffset
      if (caretOffset != null) {
        refreshControls(document, file.path, caretOffset)
      }
    }
  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    initializeTimer(project, contentManager, "PreviewUpdateTimer", this) { onTick() }
  }

  override fun dispose() {
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
    type: TypeHash
): ControlField? {
  val graph = dungeon.namespace
  val complexTypeControl = complexTypeControls[type]
  val parameters = getParameterConnections(context, node)

  return if (parameters.any()) {
    val nodes = if (complexTypeControl != null) {
      val application = graph.connections.entries.firstOrNull { it.value == node }?.key?.destination
      val arguments = getArgumentConnections(context, application ?: node)
      val signature = getTypeSignature(context, type)
      if (signature != null)
        signature.parameters.mapNotNull { parameter ->
          arguments.entries.firstOrNull { it.key.parameter == parameter.name }
              ?.value
        }
      else
        listOf()
    } else
      listOf(node)

    val psiElements = nodes.mapNotNull {
      val offset = dungeon.nodeMap[it]?.range?.start?.index
      if (offset != null) getPsiElement(offset) else null
    }
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

fun getApplicationAndTarget(context: Context, namespace: Namespace, node: PathKey): Pair<PathKey?, PathKey?> {
  return if (getArgumentConnections(context, node).size > 1)
    node to resolveReference(context, node)
  else {
    val application = namespace.connections.entries.firstOrNull { it.value == node }?.key?.destination
    application to if (application != null) resolveReference(context, application) else null
  }
}

fun getApplicationArgument(context: Context, namespace: Namespace, node: PathKey): PathKey? =
    if (getArgumentConnections(context, node).size > 1)
      resolveReference(context, node)
    else
      node

fun gatherControlFields(
    getPsiElement: GetPsiValue,
    context: Context,
    dungeon: Dungeon,
    node: PathKey
): List<ControlField> {
  val graph = dungeon.namespace
  val (application, target) = getApplicationAndTarget(context, dungeon.namespace, node)
  val functionType = graph.nodeTypes[target]
  return if (functionType == null)
    listOf()
  else {
    if (!complexTypeControls.containsKey(functionType)) {
      val connections = getArgumentConnections(context, application!!)
      connections.mapNotNull { connection ->
        val child = getApplicationArgument(context, dungeon.namespace, connection.value)
        val type = graph.nodeTypes[child]
        if (type != null)
          newFieldControl(
              getPsiElement,
              context,
              dungeon,
              child!!,
              type
          )
        else
          null
      }
    } else {
      listOfNotNull(newFieldControl(getPsiElement, context, dungeon, target!!, functionType))
    }
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
