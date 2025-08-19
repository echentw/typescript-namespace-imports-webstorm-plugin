package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.rd.util.ConcurrentHashMap

data class TsProject(
    val tsConfigJson: TsConfigJson,
    val modulesForBareImportByQueryFirstChar: Map<String, Array<ModuleForBareImport>>,
    val modulesForRelativeImportByQueryFirstChar: Map<String, Array<ModuleForRelativeImport>>,
)

data class TsConfigJson(
    val baseUrl: String?,
    val paths: Map<String, Array<String>>,
    val outDir: String?,
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
    fun getModulesForCompletion(currentFile: VirtualFile, queryFirstLetter: Char): Array<ModuleForCompletion>
}

@Service(Service.Level.PROJECT)
class NamespaceImportCompletionServiceImpl(private val project: Project): NamespaceImportCompletionService {

    private val tsProjectByPath: Map<TsProjectPath, TsProject> = ConcurrentHashMap()
    private val ownerTsProjectPathByTsFilePath: Map<TsFilePath, TsProjectPath> = ConcurrentHashMap()

    private var isInitialized = false
    override fun initialize() {
        if (!isInitialized) {
            scanFiles()
            setupFileWatcher()
            isInitialized = true
        }
    }

    fun rescanFiles() {
        if (isInitialized) {
            scanFiles()
        }
    }

    override fun getModulesForCompletion(currentFile: VirtualFile, queryFirstLetter: Char): Array<ModuleForCompletion> {
        throw Exception("todo")
    }

    private fun scanFiles() {
        val tsConfigJsonByTsProjectPath = discoverTsConfigJsons()

        val tsFiles = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project))
        val tsxFiles = FilenameIndex.getAllFilesByExt(project, "tsx", GlobalSearchScope.projectScope(project))
        val allFiles = tsFiles + tsxFiles


    }

    private fun discoverTsConfigJsons(): Map<TsProjectPath, TsConfigJson> {
        val tsConfigJsonFiles = FilenameIndex.getVirtualFilesByName("tsconfig.json", GlobalSearchScope.projectScope(project))

        throw Exception("todo")
    }

    private fun setupFileWatcher() {

    }
}
