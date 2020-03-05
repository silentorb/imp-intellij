package silentorb.imp.intellij.language

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import silentorb.imp.core.Namespace
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.execution.combineLibraries
import silentorb.imp.library.implementation.standard.standardLibrary
import silentorb.mythic.imaging.texturingLibrary

@Service
class LanguageService {
  val context: List<Namespace>

  val functions: FunctionImplementationMap

  init {
    val library = combineLibraries(
        standardLibrary(),
        texturingLibrary()
    )
    context = listOf(library.namespace)
    functions = library.implementation
  }

}

fun initialContext() =
    ServiceManager.getService(LanguageService::class.java).context

fun initialFunctions() =
    ServiceManager.getService(LanguageService::class.java).functions
