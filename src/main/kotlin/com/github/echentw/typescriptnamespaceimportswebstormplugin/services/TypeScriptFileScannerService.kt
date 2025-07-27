package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class TypeScriptFileScannerService(private val project: Project) {
    
    private val tsFileByPath = ConcurrentHashMap<String, VirtualFile>()
    
    init {
        scanForTsFiles()
    }
    
    fun getTsFileByPath(): Map<String, VirtualFile> {
        return tsFileByPath.toMap()
    }
    
    private fun scanForTsFiles() {
        tsFileByPath.clear()
        
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
            
            tsFileByPath[path] = file
            println("Found TypeScript file: $path")
        }
        
        println("Total TypeScript files found: ${tsFileByPath.size}")
    }
    
    private fun shouldIgnoreFile(path: String): Boolean {
        return path.contains("/node_modules/") ||
               path.contains("/.git/") ||
               path.contains("/build/") ||
               path.contains("/dist/") ||
               path.contains("/out/") ||
               path.contains("/.idea/")
    }
}