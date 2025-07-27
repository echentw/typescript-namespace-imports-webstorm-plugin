package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ModuleInfo(
    val moduleName: String,
    val virtualFile: VirtualFile
)

@Service(Service.Level.PROJECT)
class TypeScriptFileScannerService(private val project: Project) {
    
    private val modulesByFirstLetter = ConcurrentHashMap<String, MutableList<ModuleInfo>>()
    private var isInitialized = false
    private val debounceExecutor = Executors.newSingleThreadScheduledExecutor()
    private var pendingRescanTask: java.util.concurrent.ScheduledFuture<*>? = null
    
    fun initialize() {
        if (!isInitialized) {
            scanForModules()
            setupFileWatcher()
            isInitialized = true
        }
    }
    
    fun forceRescan() {
        if (isInitialized) {
            scanForModules()
        }
    }
    
    private fun setupFileWatcher() {
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val relevantEvents = events.filter { event ->
                    isTypeScriptFile(event) && isInProjectScope(event)
                }
                
                if (relevantEvents.isNotEmpty()) {
                    thisLogger().debug("TypeScript files changed: ${relevantEvents.size} events")
                    scheduleRescan()
                }
            }
        })
    }
    
    private fun isTypeScriptFile(event: VFileEvent): Boolean {
        val fileName = when (event) {
            is VFileCreateEvent -> event.childName
            is VFileDeleteEvent -> event.file?.name
            is VFileMoveEvent -> event.file.name
            is VFilePropertyChangeEvent -> if (event.propertyName == VirtualFile.PROP_NAME) event.newValue as? String else event.file.name
            else -> null
        }
        return fileName?.endsWith(".ts") == true || fileName?.endsWith(".tsx") == true
    }
    
    private fun isInProjectScope(event: VFileEvent): Boolean {
        val file = when (event) {
            is VFileCreateEvent -> event.parent?.findChild(event.childName)
            is VFileDeleteEvent -> event.file
            is VFileMoveEvent -> event.file
            is VFilePropertyChangeEvent -> event.file
            else -> null
        }
        
        return file?.let { 
            !shouldIgnoreFile(it.path) && 
            project.basePath?.let { basePath -> it.path.startsWith(basePath) } == true
        } ?: false
    }
    
    private fun scheduleRescan() {
        // Cancel any pending rescan
        pendingRescanTask?.cancel(false)
        
        // Schedule a new rescan with debouncing (wait 500ms after last change)
        pendingRescanTask = debounceExecutor.schedule({
            ApplicationManager.getApplication().runReadAction {
                try {
                    thisLogger().info("Rescanning TypeScript files due to file system changes")
                    scanForModules()
                } catch (e: Exception) {
                    thisLogger().error("Error during TypeScript file rescan", e)
                }
            }
        }, 500, TimeUnit.MILLISECONDS)
    }
    
    fun getModulesByFirstLetter(): Map<String, List<ModuleInfo>> {
        return modulesByFirstLetter.mapValues { it.value.toList() }
    }
    
    private fun scanForModules() {
        modulesByFirstLetter.clear()
        
        // Get TsConfigService to check for outDir exclusions
        val tsConfigService = project.getService(TsConfigService::class.java)
        
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
            
            // Skip files in TypeScript output directories
            if (shouldIgnoreFileBasedOnTsConfig(file, tsConfigService)) {
                continue
            }
            
            // Extract module name from file path
            val moduleName = makeModuleName(path)
            val moduleInfo = ModuleInfo(moduleName, file)
            
            // Add to prefix mapping for all possible prefixes
            val prefix = moduleName.substring(0, 1).lowercase()
            modulesByFirstLetter.computeIfAbsent(prefix) { mutableListOf() }.add(moduleInfo)

        }
        
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

private fun shouldIgnoreFileBasedOnTsConfig(file: VirtualFile, tsConfigService: TsConfigService): Boolean {
    val tsConfig = tsConfigService.getTsConfigForFile(file)
    
    if (tsConfig?.outDir != null) {
        val configDir = tsConfig.configFile.parent
        val outDir = configDir.findFileByRelativePath(tsConfig.outDir)
        
        if (outDir != null && file.path.startsWith(outDir.path)) {
            return true
        }
    }
    
    return false
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
