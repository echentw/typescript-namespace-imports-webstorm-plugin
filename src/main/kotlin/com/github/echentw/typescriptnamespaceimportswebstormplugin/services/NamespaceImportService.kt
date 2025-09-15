package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import evaluateModuleForTsProject
import parseTsConfigJson
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.extension

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
    val tsFilePath: TsFilePath, // used to make sure we don't suggest the same file we're in
)

data class ModuleForRelativeImport(
    val moduleName: String,
    val tsFilePath: TsFilePath,
)

typealias FilePath = String
typealias TsFilePath = String
typealias TsProjectPath = String

data class ModuleForCompletion(
    val moduleName: String,
    val importPath: String,
)

interface NamespaceImportService {
    fun getModulesForCompletion(filePath: FilePath, query: String): List<ModuleForCompletion>

    fun handleFileCreated(filePath: FilePath)
    fun handleFileDeleted(filePath: FilePath, isDirectory: Boolean)
    fun handleFileContentChanged(filePath: FilePath)
}

class NamespaceImportServiceImpl(private val project: Project) : NamespaceImportService {
    private var tsProjectByPath: MutableMap<TsProjectPath, TsProject> = ConcurrentHashMap()
    private var ownerTsProjectPathByTsFilePath: MutableMap<TsFilePath, TsProjectPath> = ConcurrentHashMap()

    init {
        Util.playground()
        reset()
    }

    private fun reset() {
        tsProjectByPath = mutableMapOf()
        ownerTsProjectPathByTsFilePath = mutableMapOf()

        val tsConfigJsonByTsProjectPath = discoverTsConfigJsons(project)
        for ((tsProjectPath, tsConfigJson) in tsConfigJsonByTsProjectPath) {
            tsProjectByPath[tsProjectPath] = TsProject(
                tsConfigJson,
                ConcurrentHashMap(),
                ConcurrentHashMap(),
            )
        }

        val tsFiles = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project))
        val tsxFiles = FilenameIndex.getAllFilesByExt(project, "tsx", GlobalSearchScope.projectScope(project))
        val allFiles = (tsFiles + tsxFiles).filter { !shouldFileBeIgnored(it.path, tsConfigJsonByTsProjectPath) }

        val tsProjectPathsSorted = tsProjectByPath.keys.sortedByDescending { it.count { char -> char == '/' } }
        for (file in allFiles) {
            processNewTsFile(file.path, tsProjectPathsSorted)
        }

//        println("tsProjectByPath: ${Util.stringify(tsProjectByPath)}")
//        println("ownerTsProjectPathByTsFilePath: ${Util.stringify(ownerTsProjectPathByTsFilePath)}")
    }

    override fun getModulesForCompletion(filePath: FilePath, query: String): List<ModuleForCompletion> {
        val tsProjectPath = ownerTsProjectPathByTsFilePath[filePath] ?: return emptyList()
        val tsProject = tsProjectByPath[tsProjectPath] ?: return emptyList()

        val modules = mutableListOf<ModuleForCompletion>()

        val modulesForBareImport = tsProject.modulesForBareImportByQueryFirstChar[query.first()] ?: emptyList()
        for (moduleInfo in modulesForBareImport) {
            if (moduleInfo.tsFilePath == filePath) {
                // Don't suggest the current file as a completion option.
                continue
            }
            modules.add(ModuleForCompletion(moduleInfo.moduleName, moduleInfo.importPath))
        }

        val modulesForRelativeImport = tsProject.modulesForRelativeImportByQueryFirstChar[query.first()] ?: emptyList()
        for (moduleInfo in modulesForRelativeImport) {
            if (moduleInfo.tsFilePath == filePath) {
                // Don't suggest the current file as a completion option.
                continue
            }
            val relativeModulePath = Path(filePath).parent.relativize(Path(moduleInfo.tsFilePath)).normalize().toString()

            var importPath = Util.pathWithoutExtension(relativeModulePath)
            if (!relativeModulePath.startsWith("..")) {
                importPath = "./$importPath"
            }

            modules.add(ModuleForCompletion(moduleInfo.moduleName, importPath))
        }

        return modules
    }

    override fun handleFileCreated(filePath: FilePath) {
        if (isTsConfigJson(filePath)) {
            reset()
            return
        }
        if (!isTsFile(filePath)) return
        if (shouldFileBeIgnored(filePath, tsProjectByPath.mapValues { it.value.tsConfigJson })) return

        val tsProjectPathsSorted = tsProjectByPath.keys.sortedByDescending { it.count { char -> char == '/' } }
        processNewTsFile(filePath, tsProjectPathsSorted)
    }

    override fun handleFileDeleted(filePath: FilePath, isDirectory: Boolean) {
        if (isDirectory) {
            for (tsProjectPath in tsProjectByPath.keys) {
                if (tsProjectPath.startsWith(filePath)) {
                    reset()
                    return
                }
            }
            if (shouldFileBeIgnored(filePath, tsProjectByPath.mapValues { it.value.tsConfigJson })) return

            for ((_, tsProject) in tsProjectByPath) {
                for ((_, modules) in tsProject.modulesForBareImportByQueryFirstChar) {
                    modules.removeAll { it.tsFilePath.startsWith(filePath) }
                }
                for ((_, modules) in tsProject.modulesForRelativeImportByQueryFirstChar) {
                    modules.removeAll { it.tsFilePath.startsWith(filePath) }
                }
            }

            val toRemove = mutableListOf<TsFilePath>()
            for (tsFilePath in ownerTsProjectPathByTsFilePath.keys) {
                if (tsFilePath.startsWith(filePath)) {
                    toRemove.add(tsFilePath)
                }
            }
            for (tsFilePath in toRemove) {
                ownerTsProjectPathByTsFilePath.remove(tsFilePath)
            }
        } else {
            if (isTsConfigJson(filePath)) {
                reset()
                return
            }
            if (!isTsFile(filePath)) return
            if (shouldFileBeIgnored(filePath, tsProjectByPath.mapValues { it.value.tsConfigJson })) return

            for ((tsProjectPath, tsProject) in tsProjectByPath) {
                when (val evalResult = evaluateModuleForTsProject(tsProjectPath, tsProject.tsConfigJson, filePath)) {
                    is ModuleEvaluationForTsProject.BareImport -> {
                        val char = evalResult.moduleName.first()
                        val modules = tsProject.modulesForBareImportByQueryFirstChar[char]
                        if (modules != null) {
                            val filteredModules = mutableListOf<ModuleForBareImport>()
                            modules.filterTo(filteredModules) { it.tsFilePath != filePath }
                            tsProject.modulesForBareImportByQueryFirstChar[char] = filteredModules
                        }
                    }
                    is ModuleEvaluationForTsProject.RelativeImport -> {
                        val char = evalResult.moduleName.first()
                        val modules = tsProject.modulesForRelativeImportByQueryFirstChar[char]
                        if (modules != null) {
                            val filteredModules = mutableListOf<ModuleForRelativeImport>()
                            modules.filterTo(filteredModules) { it.tsFilePath != filePath }
                            tsProject.modulesForRelativeImportByQueryFirstChar[char] = filteredModules
                        }
                    }
                    is ModuleEvaluationForTsProject.ImportDisallowed -> {}
                }
            }

            ownerTsProjectPathByTsFilePath.remove(filePath)
        }
    }

    override fun handleFileContentChanged(filePath: FilePath) {
        if (isTsConfigJson(filePath)) {
            reset()
        }
    }

    private fun processNewTsFile(filePath: FilePath, tsProjectPathsSorted: List<TsProjectPath>) {
        for ((tsProjectPath, tsProject) in tsProjectByPath) {
            when (val evalResult = evaluateModuleForTsProject(tsProjectPath, tsProject.tsConfigJson, filePath)) {
                is ModuleEvaluationForTsProject.BareImport -> {
                    val (moduleName, importPath) = evalResult
                    tsProject.modulesForBareImportByQueryFirstChar
                        .getOrPut(moduleName.first()) { mutableListOf() }
                        .add(ModuleForBareImport(moduleName, importPath, filePath))
                }

                is ModuleEvaluationForTsProject.RelativeImport -> {
                    val (moduleName) = evalResult
                    tsProject.modulesForRelativeImportByQueryFirstChar
                        .getOrPut(moduleName.first()) { mutableListOf() }
                        .add(ModuleForRelativeImport(moduleName, filePath))
                }

                is ModuleEvaluationForTsProject.ImportDisallowed -> {}
            }
        }

        val tsProjectPath = tsProjectPathsSorted.firstOrNull { filePath.startsWith(it) }
        if (tsProjectPath != null) {
            ownerTsProjectPathByTsFilePath[filePath] = tsProjectPath
        }
    }
}

private fun isTsConfigJson(filePath: FilePath): Boolean {
    return Paths.get(filePath).fileName.toString() == "tsconfig.json"
}

private fun isTsFile(filePath: FilePath): Boolean {
    val extension = Paths.get(filePath).extension
    return extension == "ts" || extension == "tsx"
}

private fun shouldFileBeIgnored(filePath: FilePath, tsConfigJsonByProjectPath: Map<TsProjectPath, TsConfigJson>): Boolean {
    if (filePath.contains("/node_modules/")) return true

    for ((tsProjectPath, tsConfigJson) in tsConfigJsonByProjectPath) {
        if (tsConfigJson.outDir == null) continue
        val outDirPath = Path(tsProjectPath).resolve(tsConfigJson.outDir).normalize().toString()
        if (filePath.startsWith(outDirPath)) {
            return true
        }
    }
    return false
}

private fun discoverTsConfigJsons(project: Project): Map<TsProjectPath, TsConfigJson> {
    val map = mutableMapOf<TsProjectPath, TsConfigJson>()

    val files = FilenameIndex.getVirtualFilesByName("tsconfig.json", GlobalSearchScope.projectScope(project))
    for (file in files) {
        when (val result = parseTsConfigJson(file)) {
            is Result.Err -> {
                println(result.err)
            }
            is Result.Ok -> {
                map[file.parent.path] = result.value
            }
        }
    }
    return map
}
