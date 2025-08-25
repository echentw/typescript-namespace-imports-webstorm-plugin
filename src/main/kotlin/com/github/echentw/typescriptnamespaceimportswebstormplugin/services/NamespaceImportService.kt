package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import evaluateModuleForTsProject
import parseTsConfigJson
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

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
        val allFiles = (tsFiles + tsxFiles).filter { !shouldTsFileBeIgnored(it, tsConfigJsonByTsProjectPath) }

        val tsProjectPathsSorted = tsProjectByPath.keys.sortedByDescending { it.count { char -> char == '/' } }
        for (file in allFiles) {
            processNewTsFile(file, tsProjectPathsSorted)
        }

        println("tsProjectByPath: ${Util.stringify(tsProjectByPath)}")
        println("ownerTsProjectPathByTsFilePath: ${Util.stringify(ownerTsProjectPathByTsFilePath)}")
    }

    override fun getModulesForCompletion(file: VirtualFile, query: String): List<ModuleForCompletion> {
        val tsProjectPath = ownerTsProjectPathByTsFilePath[file.path] ?: return emptyList()
        val tsProject = tsProjectByPath[tsProjectPath] ?: return emptyList()

        val modules = mutableListOf<ModuleForCompletion>()

        val modulesForBareImport = tsProject.modulesForBareImportByQueryFirstChar[query.first()] ?: emptyList()
        for (moduleInfo in modulesForBareImport) {
            if (moduleInfo.tsFilePath == file.path) {
                // Don't suggest the current file as a completion option.
                continue
            }
            modules.add(ModuleForCompletion(moduleInfo.moduleName, moduleInfo.importPath))
        }

        val modulesForRelativeImport = tsProject.modulesForRelativeImportByQueryFirstChar[query.first()] ?: emptyList()
        for (moduleInfo in modulesForRelativeImport) {
            if (moduleInfo.tsFilePath == file.path) {
                // Don't suggest the current file as a completion option.
                continue
            }
            val relativeModulePath = Path(file.path).parent.relativize(Path(moduleInfo.tsFilePath)).normalize().toString()

            var importPath = Util.pathWithoutExtension(relativeModulePath)
            if (!relativeModulePath.startsWith("..")) {
                importPath = "./$importPath"
            }

            modules.add(ModuleForCompletion(moduleInfo.moduleName, importPath))
        }

        return modules
    }

    override fun handleFileCreated(file: VirtualFile) {
        if (isTsConfigJson(file)) {
            reset()
            return
        }
        if (!isTsFile(file)) return
        if (shouldTsFileBeIgnored(file, tsProjectByPath.mapValues { it.value.tsConfigJson })) return

        val tsProjectPathsSorted = tsProjectByPath.keys.sortedByDescending { it.count { char -> char == '/' } }
        processNewTsFile(file, tsProjectPathsSorted)
    }

    override fun handleFileDeleted(file: VirtualFile) {
        if (isTsConfigJson(file)) {
            reset()
            return
        }
        if (!isTsFile(file)) return
        if (shouldTsFileBeIgnored(file, tsProjectByPath.mapValues { it.value.tsConfigJson })) return

        for ((tsProjectPath, tsProject) in tsProjectByPath) {
            when (val evalResult = evaluateModuleForTsProject(tsProjectPath, tsProject.tsConfigJson, file)) {
                is ModuleEvaluationForTsProject.BareImport -> {
                    val char = evalResult.moduleName.first()
                    val modules = tsProject.modulesForBareImportByQueryFirstChar[char]
                    if (modules != null) {
                        val filteredModules = mutableListOf<ModuleForBareImport>()
                        modules.filterTo(filteredModules) { it.tsFilePath != file.path }
                        tsProject.modulesForBareImportByQueryFirstChar[char] = filteredModules
                    }
                }
                is ModuleEvaluationForTsProject.RelativeImport -> {
                    val char = evalResult.moduleName.first()
                    val modules = tsProject.modulesForRelativeImportByQueryFirstChar[char]
                    if (modules != null) {
                        val filteredModules = mutableListOf<ModuleForRelativeImport>()
                        modules.filterTo(filteredModules) { it.tsFilePath != file.path }
                        tsProject.modulesForRelativeImportByQueryFirstChar[char] = filteredModules
                    }
                }
                is ModuleEvaluationForTsProject.ImportDisallowed -> {}
            }
        }
        ownerTsProjectPathByTsFilePath.remove(file.path)
    }

    override fun handleFileContentChanged(file: VirtualFile) {
        if (isTsConfigJson(file)) {
            reset()
        }
    }

    private fun processNewTsFile(file: VirtualFile, tsProjectPathsSorted: List<TsProjectPath>) {
        for ((tsProjectPath, tsProject) in tsProjectByPath) {
            when (val evalResult = evaluateModuleForTsProject(tsProjectPath, tsProject.tsConfigJson, file)) {
                is ModuleEvaluationForTsProject.BareImport -> {
                    val (moduleName, importPath) = evalResult
                    tsProject.modulesForBareImportByQueryFirstChar
                        .getOrPut(moduleName.first()) { mutableListOf() }
                        .add(ModuleForBareImport(moduleName, importPath, file.path))
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
            ownerTsProjectPathByTsFilePath[file.path] = tsProjectPath
        }
    }
}

private fun isTsConfigJson(file: VirtualFile): Boolean {
    return file.name == "tsconfig.json"
}

private fun isTsFile(file: VirtualFile): Boolean {
    return file.extension == "ts" || file.extension == "tsx"
}

private fun shouldTsFileBeIgnored(file: VirtualFile, tsConfigJsonByProjectPath: Map<TsProjectPath, TsConfigJson>): Boolean {
    if (file.path.contains("/node_modules/")) return true

    for ((tsProjectPath, tsConfigJson) in tsConfigJsonByProjectPath) {
        if (tsConfigJson.outDir == null) continue
        val outDirPath = Path(tsProjectPath).resolve(tsConfigJson.outDir).normalize().toString()
        if (file.path.startsWith(outDirPath)) {
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
