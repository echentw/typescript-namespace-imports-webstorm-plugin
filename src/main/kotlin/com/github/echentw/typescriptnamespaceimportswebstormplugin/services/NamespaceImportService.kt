package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap
import kotlin.String
import kotlin.collections.Map

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

interface NamespaceImportService {
    fun getModulesForCompletion(file: VirtualFile, query: String): Array<ModuleForCompletion>

    fun handleFileCreated(file: VirtualFile)
    fun handleFileDeleted(file: VirtualFile)
    fun handleFileContentChanged(file: VirtualFile)
}

class NamespaceImportServiceImpl(private val project: Project) : NamespaceImportService {
    private val tsProjectByPath: MutableMap<TsProjectPath, TsProject> = ConcurrentHashMap()
    private val ownerTsProjectPathByTsFilePath: Map<TsFilePath, TsProjectPath> = ConcurrentHashMap()

    init {
        val tsConfigJsonByTsProjectPath = discoverTsConfigJsons(project)
        for ((tsProjectPath, tsConfigJson) in tsConfigJsonByTsProjectPath) {
            tsProjectByPath.put(tsProjectPath, TsProject(
                tsConfigJson,
                ConcurrentHashMap(),
                ConcurrentHashMap(),
            ))
        }

        val tsFiles = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project))
        val tsxFiles = FilenameIndex.getAllFilesByExt(project, "tsx", GlobalSearchScope.projectScope(project))
        val allFiles = tsFiles + tsxFiles

        for (file in allFiles) {
            // 1. find the tsconfig.json and project path
            // 2. create a TsProject
            // 3.

        }
    }

    override fun getModulesForCompletion(file: VirtualFile, query: String): Array<ModuleForCompletion> {
        throw Exception("todo")
    }

    override fun handleFileCreated(file: VirtualFile) {
        throw Exception("todo")
    }

    override fun handleFileDeleted(file: VirtualFile) {
        throw Exception("todo")
    }

    override fun handleFileContentChanged(file: VirtualFile) {
        throw Exception("todo")
    }
}

private fun discoverTsConfigJsons(project: Project): Map<TsProjectPath, TsConfigJson> {
    val tsConfigJsonFiles = FilenameIndex.getVirtualFilesByName("tsconfig.json", GlobalSearchScope.projectScope(project))
    for (tsConfigJsonFile in tsConfigJsonFiles) {
        tsConfigJsonFile.path
    }

    throw Exception("todo")
}
