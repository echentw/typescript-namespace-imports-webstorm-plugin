package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap

data class ModuleInfo(
    val moduleName: String,
    val filePath: String,
    val virtualFile: VirtualFile
)

@Service(Service.Level.PROJECT)
class TypeScriptFileScannerService(private val project: Project) {
    
    private val modulesByFirstLetter = ConcurrentHashMap<String, MutableList<ModuleInfo>>()
    
    init {
        scanForModules()
    }
    
    fun getModulesByFirstLetter(): Map<String, List<ModuleInfo>> {
        return modulesByFirstLetter.mapValues { it.value.toList() }
    }
    
    private fun scanForModules() {
        modulesByFirstLetter.clear()
        
        // Find all .ts and .tsx files in the project
        val tsFiles = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project))
        val tsxFiles = FilenameIndex.getAllFilesByExt(project, "tsx", GlobalSearchScope.projectScope(project))
        
        val allFiles = tsFiles + tsxFiles
        
        for (file in allFiles) {
            val path = file.path
            
            // Skip node_modules and other irrelevant directories
            if (shouldIgnoreFile(path)) {
                continue
            }
            
            // Extract module name from file path
            val moduleName = makeModuleName(path)
            val moduleInfo = ModuleInfo(moduleName, path, file)
            
            // Add to prefix mapping for all possible prefixes
            val prefix = moduleName.substring(0, 1).lowercase()
            modulesByFirstLetter.computeIfAbsent(prefix) { mutableListOf() }.add(moduleInfo)

            println("Found TypeScript file: $path -> module name: $moduleName")
        }
        
        println("Total module prefixes: ${modulesByFirstLetter.size}")
    }
}

private fun shouldIgnoreFile(path: String): Boolean {
    return path.contains("/node_modules/") ||
            path.contains("/.git/") ||
            path.contains("/build/") ||
            path.contains("/dist/") ||
            path.contains("/out/") ||
            path.contains("/.idea/")
}

private fun makeModuleName(filePath: String): String {
    // Get the filename without extension
    val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")

    // Convert to camelCase
    return toCamelCase(fileName)
}

private fun toCamelCase(input: String): String {
    // Handle different naming conventions: snake_case, kebab-case, etc.
    return input.split("_", "-", ".")
        .filter { it.isNotEmpty() }
        .mapIndexed { index, part ->
            if (index == 0) {
                part.lowercase()
            } else {
                part.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
        .joinToString("")
}
