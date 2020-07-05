package silentorb.imp.intellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile
import silentorb.imp.campaign.*
import silentorb.imp.core.*
import silentorb.imp.execution.Library
import silentorb.imp.execution.combineLibraries
import silentorb.imp.execution.execute
import silentorb.imp.intellij.ui.misc.getDocumentFromPath
import silentorb.imp.library.standard.standardLibrary
import silentorb.imp.parsing.general.GetCode
import silentorb.imp.parsing.parser.parseToDungeon
import silentorb.mythic.aura.generation.imp.auraLibrary
import silentorb.mythic.fathom.fathomLibrary
import silentorb.mythic.imaging.texturing.texturingLibrary
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

data class DungeonArtifact(
    val response: Response<Dungeon>,
    val timestamp: Long
)

val getCodeFromDocument: GetCode = { path ->
  getDocumentFromPath(path)?.text
}

@Service
class ImpLanguageService {
  val context: List<Namespace>
  val functions: FunctionImplementationMap
  val dungeonArtifacts: WeakHashMap<PsiFile, DungeonArtifact> = WeakHashMap()
  val workspaceArtifacts: WeakHashMap<Path, Response<Workspace>> = WeakHashMap()
  val library: Library = combineLibraries(
      standardLibrary(),
      auraLibrary(),
      texturingLibrary(),
      fathomLibrary()
  )

  init {
    context = listOf(library.namespace)
    functions = library.implementation
  }

  fun getOrCreateWorkspaceArtifact(childPath: Path): Response<Workspace>? {
    val workspaceResponse = loadContainingWorkspace(getCodeFromDocument, library, childPath)
    if (workspaceResponse != null) {
      workspaceArtifacts[workspaceResponse.value.path] = workspaceResponse
    }
    return workspaceResponse
  }

  fun getArtifact(document: Document, file: PsiFile): Response<Dungeon> {
    return try {
      val existing = dungeonArtifacts[file]
      if (existing != null && existing.timestamp == document.modificationStamp)
        return existing.response

      // Lock down the timestamp in case it changes while parsing.
      val timestamp = document.modificationStamp
      val actualFile = FileDocumentManager.getInstance().getFile(document)!!
      val filePath = Paths.get(actualFile.path)

      println("New artifact: $filePath")
      val workspaceResponse = getOrCreateWorkspaceArtifact(filePath)
      val moduleDirectory = findContainingModule(filePath)
      val response = if (workspaceResponse != null && moduleDirectory != null) {
        val (workspace, parsingErrors) = workspaceResponse
        val moduleName = moduleDirectory.fileName.toString()
        val fileName = filePath.fileName.toString().split(".").first()
        val module = workspace.modules[moduleName]
        if (module != null) {
          println("Sending artifact: $filePath")
          println("Hashes ${existing?.response?.value?.hashCode() ?: "none"} ${module.dungeons[fileName]?.hashCode() ?: "none"}")
          println(module.dungeons[fileName]?.graph?.values?.values?.last())
          Response(
              module.dungeons[fileName] ?: emptyDungeon,
              parsingErrors
          )
        } else
          Response(
              emptyDungeon,
              parsingErrors
          )
      } else {
        val (dungeon, dungeonResponse) = parseToDungeon(actualFile.path, context)(document.text)
        Response(
            dungeon.copy(
                graph = mergeNamespaces(context + dungeon.graph)
            ),
            dungeonResponse
        )
      }

      val artifact = DungeonArtifact(
          response = response,
          timestamp = timestamp
      )
      dungeonArtifacts[file] = artifact
      return response
    } catch (error: Error) {
      Response(emptyDungeon, listOf())
    }
  }
}

fun getImpLanguageService(): ImpLanguageService =
    ServiceManager.getService(ImpLanguageService::class.java)

fun initialContext() =
    getImpLanguageService().context

fun initialFunctions() =
    getImpLanguageService().functions

fun getExistingArtifact(file: PsiFile): Response<Dungeon>? =
    getImpLanguageService().dungeonArtifacts[file]?.response

fun getWorkspaceArtifact(path: Path): Response<Workspace>? =
    getImpLanguageService().getOrCreateWorkspaceArtifact(path)

fun executeGraph(file: Path, functions: FunctionImplementationMap, graph: Graph, node: PathKey?): Any? {
  val output = node ?: getGraphOutputNode(graph)
  return if (output == null)
    null
  else {
    val workspaceResponse = getWorkspaceArtifact(file)
    val (context, functions2) = if (workspaceResponse != null) {
      getModulesExecutionArtifacts(functions, initialContext(), workspaceResponse.value.modules)
    } else
      Pair(listOf(graph), functions)

    val values = execute(context, functions2, setOf(output))
    values[output]
  }
}
