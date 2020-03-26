package silentorb.imp.intellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import silentorb.imp.core.*
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.execution.combineLibraries
import silentorb.imp.library.implementation.standard.standardLibrary
import silentorb.imp.parsing.general.PartitionedResponse
import silentorb.imp.parsing.parser.Dungeon
import silentorb.imp.parsing.parser.parseText
import silentorb.mythic.imaging.texturingLibrary
import java.util.*

val scaleLengthKey = PathKey("silentorb.mythic.injected", "scaleLength")
val scaleLengthSignature = Signature(
    parameters = listOf(
        Parameter("length", intKey)
    ),
    output = intKey
)

data class DungeonArtifact(
    val response: PartitionedResponse<Dungeon>,
    val timestamp: Long
)

@Service
class ImpLanguageService {
  val context: List<Namespace>
  val functions: FunctionImplementationMap
  val artifacts: WeakHashMap<PsiFile, DungeonArtifact> = WeakHashMap()

  init {
    val library = combineLibraries(
        standardLibrary(),
        texturingLibrary()
    )
    context = listOf(
        library.namespace.copy(
            functions = library.namespace.functions.plus(
                mapOf(
                    scaleLengthKey to listOf(
                        scaleLengthSignature
                    )
                )
            )
        )
    )
    functions = library.implementation
  }

  fun getArtifact(document: Document, file: PsiFile): PartitionedResponse<Dungeon> {
    val existing = artifacts[file]
    if (existing != null && existing.timestamp == document.modificationStamp)
      return existing.response

    // Lock down the timestamp in case it changes while parsing.
    val timestamp = document.modificationStamp
    val response = parseText(context)(document.text)
    val artifact = DungeonArtifact(
        response = response,
        timestamp = timestamp
    )
    artifacts[file] = artifact
    return response
  }

}

fun getImpLanguageService() =
    ServiceManager.getService(ImpLanguageService::class.java)

fun initialContext() =
    getImpLanguageService().context

fun initialFunctions() =
    getImpLanguageService().functions

fun getExistingArtifact(file: PsiFile): PartitionedResponse<Dungeon>? =
    getImpLanguageService().artifacts[file]?.response
