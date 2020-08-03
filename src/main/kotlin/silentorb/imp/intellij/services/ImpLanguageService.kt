package silentorb.imp.intellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
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

typealias DungeonArtifact = ArtifactEntry<Response<Dungeon>>

val getCodeFromDocument: GetCode = { path ->
  getDocumentFromPath(path)?.text
}

@Service
class ImpLanguageService {
  val dungeonArtifacts: TimedArtifactCache<Path, Response<Dungeon>> = ArtifactCache()
  val workspaceArtifacts: ArtifactCache<Path, Response<Workspace>> = ArtifactCache()

  val context: List<Namespace> = listOf(
      defaultImpNamespace(),
      standardLibrary(),
      auraLibrary(),
      texturingLibrary(),
      fathomLibrary()
  )

  fun getOrCreateWorkspaceArtifact(childPath: Path): Response<Workspace>? {
    val workspaceDirectory = getContainingWorkspaceDirectory(childPath)
    return if (workspaceDirectory == null)
      null
    else
      getArtifact(workspaceArtifacts, workspaceDirectory) { key ->
        logExecutionTime("loadWorkspace") {
          loadWorkspace(key)
        }
      }
  }

  fun getArtifact(document: Document): Response<Dungeon> {
    val actualFile = FileDocumentManager.getInstance().getFile(document)!!
    val filePath = Paths.get(actualFile.path)
    return getArtifact(dungeonArtifacts, filePath, document.modificationStamp) { key ->
      val response = processWorkspaceOrModule(context, key, actualFile, document)
      println("Caching artifact: $filePath ${response.value.namespace.hashCode()}")
      response
    }
  }
}

fun getImpLanguageService(): ImpLanguageService =
    ServiceManager.getService(ImpLanguageService::class.java)

fun processWorkspaceOrModule(context: Context, filePath: Path, actualFile: VirtualFile, document: Document): Response<Dungeon> {
  try {
    val workspaceResponse = getImpLanguageService().getOrCreateWorkspaceArtifact(filePath)
    val moduleDirectory = getContainingModule(filePath)
    return if (workspaceResponse != null && moduleDirectory != null) {
      val (workspace, parsingErrors) = workspaceResponse
      val moduleName = moduleDirectory.fileName.toString()
      val fileName = filePath.fileName.toString().split(".").first()
      val modules = getWorkspaceModules(workspace)
      val module = modules[moduleName]
      if (module != null) {
        println("Returning artifact: $filePath")
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

fun initialContext() =
    getImpLanguageService().context

fun getWorkspaceModules(workspace: Workspace): ModuleMap {
  return loadModules(workspace, initialContext(), getCodeFromDocument).value
}

fun getWorkspaceArtifact(path: Path): Response<Workspace>? =
    getImpLanguageService().getOrCreateWorkspaceArtifact(path)

fun executeGraph(file: Path, graph: Namespace, node: PathKey?): Any? {
  val output = node ?: getGraphOutputNode(graph)
  return if (output == null)
    null
  else {
    val workspaceResponse = getWorkspaceArtifact(file)
    val context = if (workspaceResponse != null) {
      val modules = getWorkspaceModules(workspaceResponse.value)
      getModulesExecutionArtifacts(initialContext(), modules)
    } else
      listOf(graph)

    val values = execute(context, setOf(output))
    values[output]
  }
}

typealias DependencyState = Map<String, Int>

fun getDependencyState(document: Document?): DependencyState {
  if (document == null)
    return mapOf()

  val file = FileDocumentManager.getInstance().getFile(document)!!
  val filePath = Paths.get(file.path)

  val workspaceResponse = getWorkspaceArtifact(filePath)
  return if (workspaceResponse == null) {
    val dungeonResponse = getImpLanguageService().getArtifact(document)
    mapOf(file.path to dungeonResponse.value.namespace.hashCode())
  } else if (workspaceResponse.errors.any()) {
    mapOf()
  } else {
    val module = getContainingModule(filePath)
    if (module == null)
      mapOf()
    else {
      val workspace = workspaceResponse.value
      val moduleName = module.fileName.toString()
      val dependencies = getCascadingDependencies(workspace.dependencies, setOf(moduleName))
          .plus(moduleName)

      val modules = getWorkspaceModules(workspace)
      dependencies.associateWith { modules[it]!!.dungeons.values.first().namespace.hashCode() }
    }
  }
}
