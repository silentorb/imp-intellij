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
import java.util.concurrent.locks.ReentrantLock

val getCodeFromDocument: GetCode = { path ->
  getDocumentFromPath(path)?.text
}

@Service
class ImpLanguageService {
  val moduleArtifacts: TimedArtifactCache<ModuleId, Response<Module>> = mutableMapOf()
  val fileArtifacts: TimedArtifactCache<Path, Response<Dungeon>> = mutableMapOf()
  val workspaceArtifacts: ArtifactCache<Path, Response<Workspace>> = mutableMapOf()

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
    return processWorkspaceOrFile(context, filePath, actualFile, document)
//    return getArtifact(moduleArtifacts, filePath, document.modificationStamp) { key ->
//      val response =
//      println("Caching artifact: $filePath ${response.value.namespace.hashCode()}")
//      response
//    }
  }
}

fun getImpLanguageService(): ImpLanguageService =
    ServiceManager.getService(ImpLanguageService::class.java)

private val loadingLock = ReentrantLock()

fun processWorkspaceOrFile(context: Context, filePath: Path, actualFile: VirtualFile, document: Document): Response<Dungeon> {
  loadingLock.lock()
  return try {
    val workspaceResponse = getImpLanguageService().getOrCreateWorkspaceArtifact(filePath)
    val moduleDirectory = getContainingModule(filePath)
    if (workspaceResponse != null && moduleDirectory != null) {
      val (workspace, parsingErrors) = workspaceResponse
      val moduleName = moduleDirectory.fileName.toString()
      val fileName = filePath.fileName.toString().split(".").first()
      val moduleArtifacts = getImpLanguageService().moduleArtifacts
      val neededModules = getCascadingDependencies(workspace.dependencies, setOf(moduleName))
          .plus(moduleName)
          .map { it to workspace.modules[it]!! }
      val timestamp = document.modificationStamp
      val getModule: GetModule = { localContext, name, info ->
        getArtifact(moduleArtifacts, name, timestamp) { key ->
          println("Loading module artifact: $name")
          loadModule(getCodeFromDocument)(localContext, name, info)
        }
      }
      val (modules, errors) = loadModules(getModule, workspace.path.toUri(), neededModules, initialContext())
      val module = modules[moduleName]
      if (module != null) {
//          println("Hashes ${existing?.response?.value?.hashCode() ?: "none"} ${module.dungeons[fileName]?.hashCode() ?: "none"}")
//        println(module.dungeons[fileName]?.namespace?.values?.values?.last())
        Response(
            module.dungeons.values.firstOrNull() ?: emptyDungeon,
            parsingErrors+ errors
        )
      } else
        Response(
            emptyDungeon,
            parsingErrors + errors
        )
    } else {
      val fileArtifacts = getImpLanguageService().fileArtifacts
      getArtifact(fileArtifacts, filePath, document.modificationStamp) { key ->
        val (dungeon, dungeonResponse) = parseToDungeon(actualFile.path, context)(document.text)
        Response(
            dungeon.copy(
                namespace = mergeNamespaces(context + dungeon.namespace)
            ),
            dungeonResponse
        )
      }
    }
  } catch (error: Error) {
    Logger.getInstance(ImpLanguageService::class.java).error(error)
    Response(emptyDungeon, listOf())
  } finally {
    loadingLock.unlock()
  }
}

fun initialContext() =
    getImpLanguageService().context

fun getWorkspaceModules(workspace: Workspace): ModuleMap {
  return loadAllModules(workspace, initialContext(), getCodeFromDocument).value
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

//typealias DependencyState = Map<String, Int>
//
//fun getDependencyState(document: Document?): DependencyState {
//  if (document == null)
//    return mapOf()
//
//  val file = FileDocumentManager.getInstance().getFile(document)!!
//  val filePath = Paths.get(file.path)
//
//  val workspaceResponse = getWorkspaceArtifact(filePath)
//  return if (workspaceResponse == null) {
//    val dungeonResponse = getImpLanguageService().getArtifact(document)
//    mapOf(file.path to dungeonResponse.value.namespace.hashCode())
//  } else if (workspaceResponse.errors.any()) {
//    mapOf()
//  } else {
//    val module = getContainingModule(filePath)
//    if (module == null)
//      mapOf()
//    else {
//      val workspace = workspaceResponse.value
//      val moduleName = module.fileName.toString()
//      val dependencies = getCascadingDependencies(workspace.dependencies, setOf(moduleName))
//          .plus(moduleName)
//
//      val modules = getWorkspaceModules(workspace)
//      dependencies.associateWith { modules[it]!!.dungeons.values.first().namespace.hashCode() }
//    }
//  }
//}
