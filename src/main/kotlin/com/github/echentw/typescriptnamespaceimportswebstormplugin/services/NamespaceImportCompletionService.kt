package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.ConcurrentHashMap
import java.util.Optional

data class TsProject(
    val tsConfigJson: TsConfigJson,
    val modulesForBareImportByQueryFirstChar: Map<String, Array<ModuleForBareImport>>,
    val modulesForRelativeImportByQueryFirstChar: Map<String, Array<ModuleForRelativeImport>>,
)

data class TsConfigJson(
    val baseUrl: Optional<String>,
    val paths: Map<String, Array<String>>,
    val outDir: Optional<String>,
)

data class ModuleForBareImport(
    val moduleName: String,
    val importPath: String,
    val tsFilePath: TsFilePath,
)

data class ModuleForRelativeImport(
    val moduleName: String,
    val importPath: String,
    val tsFilePath: TsFilePath,
)

typealias TsFilePath = String
typealias TsProjectPath = String

data class ModuleForCompletion(
    val moduleName: String,
    val importPath: String,
)

interface NamespaceImportCompletionService {
    fun initialize()
    fun getModulesByFirstLetter(letter: Char): Array<ModuleForCompletion>
}

@Service(Service.Level.PROJECT)
class NamespaceImportCompletionServiceImpl(private val project: Project): NamespaceImportCompletionService {

    private val tsProjectByPath: Map<TsProjectPath, TsProject> = ConcurrentHashMap()
    private val ownerTsProjectPathByTsFilePath: Map<TsFilePath, TsProjectPath> = ConcurrentHashMap()

    override fun initialize() {
    }

    override fun getModulesByFirstLetter(letter: Char): Array<ModuleForCompletion> {
        throw Exception("TODO")
    }

}
