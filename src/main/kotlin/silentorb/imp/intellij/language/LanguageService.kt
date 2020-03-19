package silentorb.imp.intellij.language

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import silentorb.imp.core.*
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.execution.combineLibraries
import silentorb.imp.library.implementation.standard.standardLibrary
import silentorb.mythic.imaging.texturingLibrary

val scaleLengthKey = PathKey("silentorb.mythic.injected", "scaleLength")
val scaleLengthSignature = Signature(
    parameters = listOf(
        Parameter("length", intKey)
    ),
    output = intKey
)

@Service
class LanguageService {
  val context: List<Namespace>

  val functions: FunctionImplementationMap

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

}

fun initialContext() =
    ServiceManager.getService(LanguageService::class.java).context

fun initialFunctions() =
    ServiceManager.getService(LanguageService::class.java).functions
