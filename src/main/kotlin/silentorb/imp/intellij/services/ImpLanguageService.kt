package silentorb.imp.intellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.openapi.diagnostic.Logger
import silentorb.imp.campaign.*
import silentorb.imp.core.*
import silentorb.imp.execution.execute
import silentorb.imp.intellij.ui.misc.getDocumentFromPath
import silentorb.imp.library.standard.standardLibrary
import silentorb.imp.parsing.general.GetCode
import silentorb.imp.parsing.parser.parseToDungeon
import silentorb.mythic.aura.generation.imp.auraLibrary
import silentorb.mythic.debugging.logExecutionTime
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
  val dungeonArtifacts: WeakHashMap<PsiFile, DungeonArtifact> = WeakHashMap()
  val workspaceArtifacts: WeakHashMap<Path, Response<Workspace>> = WeakHashMap()
  val context: List<Namespace> = listOf(
      defaultImpNamespace(),
      standardLibrary(),
      auraLibrary(),
      texturingLibrary(),
      fathomLibrary()
  )

  fun getOrCreateWorkspaceArtifact(childPath: Path): Response<Workspace>? {
    val workspaceResponse = logExecutionTime("loadWorkspace") {
      loadContainingWorkspace(getCodeFromDocument, context, childPath)
    }
    if (workspaceResponse != null) {
      workspaceArtifacts[workspaceResponse.value.path] = workspaceResponse
    }
    return workspaceResponse
  }

  fun processWorkspaceOrModule(filePath: Path, actualFile: VirtualFile, document: Document): Response<Dungeon> {
    try {
      val workspaceResponse = getOrCreateWorkspaceArtifact(filePath)
      val moduleDirectory = findContainingModule(filePath)
      return if (workspaceResponse != null && moduleDirectory != null) {
        val (workspace, parsingErrors) = workspaceResponse
        val moduleName = moduleDirectory.fileName.toString()
        val fileName = filePath.fileName.toString().split(".").first()
        val module = workspace.modules[moduleName]
        if (module != null) {
          println("Sending artifact: $filePath")
//          println("Hashes ${existing?.response?.value?.hashCode() ?: "none"} ${module.dungeons[fileName]?.hashCode() ?: "none"}")
          println(module.dungeons[fileName]?.namespace?.values?.values?.last())
          Response(
              module.dungeons.values.firstOrNull() ?: emptyDungeon,
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
                namespace = mergeNamespaces(context + dungeon.namespace)
            ),
            dungeonResponse
        )
      }
    } catch (error: Error) {
      Logger.getInstance(ImpLanguageService::class.java).error(error)
      return Response(emptyDungeon, listOf())
    }
  }

  fun getArtifact(document: Document, file: PsiFile): Response<Dungeon> {
    val existing = dungeonArtifacts[file]
    if (existing != null && existing.timestamp == document.modificationStamp)
      return existing.response

    // Lock down the timestamp in case it changes while parsing.
    val timestamp = document.modificationStamp
    val actualFile = FileDocumentManager.getInstance().getFile(document)!!
    val filePath = Paths.get(actualFile.path)

    println("New artifact: $filePath")

    val response = processWorkspaceOrModule(filePath, actualFile, document)
    val artifact = DungeonArtifact(
        response = response,
        timestamp = timestamp
    )
    println("Caching artifact: $filePath")
    dungeonArtifacts[file] = artifact
    return response
  }
}

fun getImpLanguageService(): ImpLanguageService =
    ServiceManager.getService(ImpLanguageService::class.java)

fun initialContext() =
    getImpLanguageService().context

fun getExistingArtifact(file: PsiFile): Response<Dungeon>? =
    getImpLanguageService().dungeonArtifacts[file]?.response

fun getWorkspaceArtifact(path: Path): Response<Workspace>? =
    getImpLanguageService().getOrCreateWorkspaceArtifact(path)

fun executeGraph(file: Path, graph: Namespace, node: PathKey?): Any? {
  val output = node ?: getGraphOutputNode(graph)
  return if (output == null)
    null
  else {
    val workspaceResponse = getWorkspaceArtifact(file)
    val context= if (workspaceResponse != null) {
      getModulesExecutionArtifacts(initialContext(), workspaceResponse.value.modules)
    } else
      listOf(graph)

    val values = execute(context, setOf(output))
    values[output]
  }
}
