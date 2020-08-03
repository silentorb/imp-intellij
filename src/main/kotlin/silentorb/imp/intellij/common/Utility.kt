package silentorb.imp.intellij.common

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import silentorb.imp.campaign.getContainingModule
import silentorb.imp.campaign.getModulesExecutionArtifacts
import silentorb.imp.core.Dungeon
import silentorb.imp.core.PathKey
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.ExecutionUnit
import silentorb.imp.execution.prepareExecutionUnit
import silentorb.imp.intellij.services.*
import java.nio.file.Path
import java.nio.file.Paths

inline fun <reified T : Enum<T>> getPersistentEnumValue(key: String, default: T): T {
  val name = PropertiesComponent.getInstance().getValue(key)
  return enumValues<T>().firstOrNull { it.name == name } ?: default
}

inline fun <reified T : Enum<T>> setPersistentEnumValue(key: String): (T) -> Unit = { value ->
  PropertiesComponent.getInstance().setValue(key, value.name)
}

fun getDocumentPath(document: Document): Path =
    Paths.get(FileDocumentManager.getInstance().getFile(document)!!.path)

fun getOutputNode(document: Document?, node: PathKey?, dungeon: Dungeon): PathKey? {
  return node ?: if (document != null)
    getGraphOutputNode(dungeon, getDocumentFile(document)?.canonicalPath!!)
  else
    null
}

fun getExecutionSteps(document: Document, output: PathKey, dungeon: Dungeon): ExecutionUnit? {
  val filePath = getDocumentPath(document)
  val workspaceResponse = getWorkspaceArtifact(filePath)
  val moduleDirectory = getContainingModule(filePath)
  try {
    return if (workspaceResponse != null && moduleDirectory != null &&
        workspaceResponse.value.modules.containsKey(moduleDirectory.fileName.toString())) {
      val context = getModulesExecutionArtifacts(
          initialContext(),
          getWorkspaceModules(workspaceResponse.value)
      )
      prepareExecutionUnit(context, output)
    } else
      prepareExecutionUnit(listOf(dungeon.namespace), output)
  } catch (error: Error) {
    Logger.getInstance(ImpLanguageService::class.java).error(error)
    return null
  }
}
