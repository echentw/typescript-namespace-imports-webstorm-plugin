package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import parseTsConfigJson
import java.util.concurrent.ConcurrentHashMap
import kotlin.String
import kotlin.collections.Map

data class TsProject(
    val tsConfigJson: TsConfigJson,
    val modulesForBareImportByQueryFirstChar: MutableMap<Char, MutableList<ModuleForBareImport>>,
    val modulesForRelativeImportByQueryFirstChar: MutableMap<Char, MutableList<ModuleForRelativeImport>>,
)

data class TsConfigJson(
    val baseUrl: String?,
    val paths: Map<String, List<String>>?,
    val outDir: String?,
)

data class ModuleForBareImport(
    val moduleName: String,
    val importPath: String,
)

data class ModuleForRelativeImport(
    val moduleName: String,
    val tsFilePath: TsFilePath,
)

typealias TsFilePath = String
typealias TsProjectPath = String

data class ModuleForCompletion(
    val moduleName: String,
    val importPath: String,
)

interface NamespaceImportService {
    fun getModulesForCompletion(file: VirtualFile, query: String): List<ModuleForCompletion>

    fun handleFileCreated(file: VirtualFile)
    fun handleFileDeleted(file: VirtualFile)
    fun handleFileContentChanged(file: VirtualFile)
}

class NamespaceImportServiceImpl(private val project: Project) : NamespaceImportService {
    private val tsProjectByPath: MutableMap<TsProjectPath, TsProject> = ConcurrentHashMap()
    private val ownerTsProjectPathByTsFilePath: MutableMap<TsFilePath, TsProjectPath> = ConcurrentHashMap()

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
        val allFiles = (tsFiles + tsxFiles).filter { !shouldTsFileBeIgnored(it, tsConfigJsonByTsProjectPath) }

        val tsProjectPathsSorted = tsProjectByPath.keys.sortedByDescending { it.count { char -> char == '/'} }
        for (file in allFiles) {
            processNewTsFile(file, tsProjectPathsSorted)
        }
    }

    override fun getModulesForCompletion(file: VirtualFile, query: String): List<ModuleForCompletion> {
        val tsProjectPath = ownerTsProjectPathByTsFilePath[file.path]
        if (tsProjectPath == null) return emptyList()

        val tsProject = tsProjectByPath[tsProjectPath]
        if (tsProject == null) return emptyList()

        val modules = mutableListOf<ModuleForCompletion>()

        val modulesForBareImport = tsProject.modulesForBareImportByQueryFirstChar[query.first()] ?: emptyList()
        for (moduleInfo in modulesForBareImport) {
            modules.add(ModuleForCompletion(moduleInfo.moduleName, moduleInfo.importPath))
        }

        val modulesForRelativeImport = tsProject.modulesForRelativeImportByQueryFirstChar[query.first()] ?: emptyList()
        for (moduleInfo in modulesForBareImport) {
            // TODO
        }

        return modules
    }

    override fun handleFileCreated(file: VirtualFile) {
        if (isTsConfigJson(file)) {
            return
        }
        if (isTsFile(file)) {
            val tsProjectPathsSorted = tsProjectByPath.keys.sortedByDescending { it.count { char -> char == '/'} }
            processNewTsFile(file, tsProjectPathsSorted)
        }
    }

    override fun handleFileDeleted(file: VirtualFile) {
        // TODO: finish implementing
    }

    override fun handleFileContentChanged(file: VirtualFile) {
        // TODO: finish implementing
    }

    private fun processNewTsFile(file: VirtualFile, tsProjectPathsSorted: List<TsProjectPath>): Unit {
        for ((tsProjectPath, tsProject) in tsProjectByPath) {
            val evalResult = evaluateModuleForTsProject(tsProjectPath, tsProject, file)
            when (evalResult) {
                is ModuleEvaluationForTsProject.BareImport -> {
                    val (moduleName, importPath) = evalResult
                    tsProject.modulesForBareImportByQueryFirstChar
                        .getOrPut(moduleName.first()) { mutableListOf() }
                        .add(ModuleForBareImport(moduleName, importPath))
                }
                is ModuleEvaluationForTsProject.RelativeImport -> {
                    val (moduleName) = evalResult
                    tsProject.modulesForRelativeImportByQueryFirstChar
                        .getOrPut(moduleName.first()) { mutableListOf() }
                        .add(ModuleForRelativeImport(moduleName, file.path))
                }
                is ModuleEvaluationForTsProject.ImportDisallowed -> {}
            }
        }

        val tsProjectPath = tsProjectPathsSorted.firstOrNull { file.path.startsWith(it) }
        if (tsProjectPath != null) {
            ownerTsProjectPathByTsFilePath.put(file.path, tsProjectPath)
        }
    }
}

private fun isTsConfigJson(file: VirtualFile): Boolean {
    return file.name === "tsconfig.json"
}

private fun isTsFile(file: VirtualFile): Boolean {
    return file.extension === "ts" || file.extension === "tsx"
}

private fun shouldTsFileBeIgnored(file: VirtualFile, tsConfigJsonByProjectPath: Map<TsProjectPath, TsConfigJson>): Boolean {
    // TODO: finish implementing
    return file.path.contains("/node_modules/")
}

private fun discoverTsConfigJsons(project: Project): Map<TsProjectPath, TsConfigJson> {
    val map = mutableMapOf<TsProjectPath, TsConfigJson>()

    val files = FilenameIndex.getVirtualFilesByName("tsconfig.json", GlobalSearchScope.projectScope(project))
    for (file in files) {
        val result = parseTsConfigJson(file)
        when (result) {
            is Result.Err -> {
                println(result.err)
            }
            is Result.Ok -> {
                map.put(file.parent.path, result.value)
            }
        }
    }
    return map
}

sealed class ModuleEvaluationForTsProject {
    data class BareImport(val moduleName: String, val importPath: String) : ModuleEvaluationForTsProject()
    data class RelativeImport(val moduleName: String) : ModuleEvaluationForTsProject()
    class ImportDisallowed : ModuleEvaluationForTsProject()
}

private fun evaluateModuleForTsProject(
    tsProjectPath: TsProjectPath,
    tsProject: TsProject,
    module: VirtualFile,
): ModuleEvaluationForTsProject {
    // TODO: finish implementing
    return ModuleEvaluationForTsProject.BareImport("moduleName", "import/path")
//    return ModuleEvaluationForTsProject.ImportDisallowed()
}
