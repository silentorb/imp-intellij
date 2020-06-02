package silentorb.imp.intellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile
import silentorb.imp.campaign.CampaignResponse
import silentorb.imp.campaign.Workspace
import silentorb.imp.campaign.findContainingWorkspaceDirectory
import silentorb.imp.campaign.loadContainingWorkspace
import silentorb.imp.core.*
import silentorb.imp.execution.Library
import silentorb.imp.execution.combineLibraries
import silentorb.imp.library.standard.standardLibrary
import silentorb.imp.parsing.general.ParsingResponse
import silentorb.imp.parsing.parser.parseToDungeon
import silentorb.mythic.aura.generation.imp.auraLibrary
import silentorb.mythic.imaging.fathoming.fathomLibrary
import silentorb.mythic.imaging.texturing.texturingLibrary
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

data class DungeonArtifact(
    val response: ParsingResponse<Dungeon>,
    val timestamp: Long
)

@Service
class ImpLanguageService {
  val context: List<Namespace>
  val functions: FunctionImplementationMap
  val dungeonArtifacts: WeakHashMap<PsiFile, DungeonArtifact> = WeakHashMap()
  val workspaceArtifacts: WeakHashMap<Path, CampaignResponse<Workspace>> = WeakHashMap()
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

  fun getArtifact(document: Document, file: PsiFile): ParsingResponse<Dungeon> {
    val existing = dungeonArtifacts[file]
    if (existing != null && existing.timestamp == document.modificationStamp)
      return existing.response

    // Lock down the timestamp in case it changes while parsing.
    val timestamp = document.modificationStamp
    val actualFile = FileDocumentManager.getInstance().getFile(document)!!
    val filePath = Paths.get(actualFile.path)

    val loadingResponse = loadContainingWorkspace(library, filePath)
    val response = if (loadingResponse != null) {
      val (moduleDirectory, workspaceResponse) = loadingResponse
      val (workspace, _, parsingErrors) = workspaceResponse
      workspaceArtifacts[workspace.path] = workspaceResponse
      val moduleName = moduleDirectory.fileName.toString()
      val fileName = filePath.fileName.toString().split(".").first()
      ParsingResponse(
          workspace.modules[moduleName]!!.dungeons[fileName] ?: emptyDungeon,
          parsingErrors
      )
    } else {
      parseToDungeon(actualFile.path, context)(document.text)
    }

    val artifact = DungeonArtifact(
        response = response,
        timestamp = timestamp
    )
    dungeonArtifacts[file] = artifact
    return response
  }

}

fun getImpLanguageService() =
    ServiceManager.getService(ImpLanguageService::class.java)

fun initialContext() =
    getImpLanguageService().context

fun initialFunctions() =
    getImpLanguageService().functions

fun getExistingArtifact(file: PsiFile): ParsingResponse<Dungeon>? =
    getImpLanguageService().dungeonArtifacts[file]?.response

fun getWorkspaceArtifact(path: Path): CampaignResponse<Workspace>? =
    getImpLanguageService().workspaceArtifacts[findContainingWorkspaceDirectory(path)]
