package silentorb.imp.intellij.language

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import silentorb.imp.core.Namespace
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.library.implementation.standard.standardLibraryImplementation
import silentorb.imp.library.standard.standardLibraryNamespace
import silentorb.mythic.imaging.texturingFunctions

@Service
class LanguageService {
    val context: List<Namespace>

    val functions: FunctionImplementationMap

    init {
        val imaging = texturingFunctions()
        val namespace = standardLibraryNamespace()
        context = listOf(
            namespace
                .copy(
                    functions = namespace.functions.plus(imaging.interfaces)
                )
        )
        functions = standardLibraryImplementation()
            .plus(imaging.implementation)
    }

}

fun initialContext() =
    ServiceManager.getService(LanguageService::class.java).context

fun initialFunctions() =
    ServiceManager.getService(LanguageService::class.java).functions
