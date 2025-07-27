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
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class TsConfigInfo(
    val configFile: VirtualFile,
    val baseUrl: String?,
    val paths: Map<String, List<String>>?,
    val outDir: String?,
    val rootDir: String?
)

@Service(Service.Level.PROJECT)
class TsConfigService(private val project: Project) {
    
    private val tsConfigs = ConcurrentHashMap<String, TsConfigInfo>()
    private val gson = Gson()
    private var isInitialized = false
    private val debounceExecutor = Executors.newSingleThreadScheduledExecutor()
    private var pendingRescanTask: java.util.concurrent.ScheduledFuture<*>? = null
    
    fun initialize() {
        if (!isInitialized) {
            scanForTsConfigs()
            setupFileWatcher()
            isInitialized = true
        }
    }
    
    private fun setupFileWatcher() {
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val relevantEvents = events.filter { event ->
                    isTsConfigFile(event) && isInProjectScope(event)
                }
                
                if (relevantEvents.isNotEmpty()) {
                    thisLogger().debug("tsconfig.json files changed: ${relevantEvents.size} events")
                    scheduleRescan()
                }
            }
        })
    }
    
    private fun isTsConfigFile(event: VFileEvent): Boolean {
        val fileName = when (event) {
            is VFileCreateEvent -> event.childName
            is VFileDeleteEvent -> event.file.name
            is VFileMoveEvent -> event.file.name
            is VFileContentChangeEvent -> event.file.name
            else -> null
        }
        return fileName == "tsconfig.json"
    }
    
    private fun isInProjectScope(event: VFileEvent): Boolean {
        val file = when (event) {
            is VFileCreateEvent -> event.parent.findChild(event.childName)
            is VFileDeleteEvent -> event.file
            is VFileMoveEvent -> event.file
            is VFileContentChangeEvent -> event.file
            else -> null
        }
        
        return file?.let { 
            project.basePath?.let { basePath -> it.path.startsWith(basePath) } == true
        } ?: false
    }
    
    private fun scheduleRescan() {
        // Cancel any pending rescan
        pendingRescanTask?.cancel(false)
        
        // Schedule incremental update with debouncing (wait 1000ms after last change for tsconfig)
        pendingRescanTask = debounceExecutor.schedule({
            ApplicationManager.getApplication().runReadAction {
                try {
                    thisLogger().info("Processing tsconfig.json changes - triggering incremental TypeScript rescan")
                    
                    // For tsconfig changes, we need to do a full TypeScript rescan since
                    // filtering rules might have changed, affecting which files are included
                    val fileScanner = project.getService(TypeScriptFileScannerService::class.java)
                    
                    // First update tsconfig data
                    scanForTsConfigs()
                    
                    // Then trigger a full TypeScript rescan (can't be incremental since 
                    // tsconfig changes affect which files should be included/excluded)
                    fileScanner.forceRescan()
                } catch (e: Exception) {
                    thisLogger().error("Error during tsconfig.json update", e)
                }
            }
        }, 1000, TimeUnit.MILLISECONDS)
    }
    
    
    fun getTsConfigForFile(file: VirtualFile): TsConfigInfo? {
        // Find the closest tsconfig.json by walking up the directory tree
        var currentDir = file.parent
        while (currentDir != null) {
            val key = currentDir.path
            if (tsConfigs.containsKey(key)) {
                return tsConfigs[key]
            }
            currentDir = currentDir.parent
        }
        return null
    }
    
    private fun scanForTsConfigs() {
        tsConfigs.clear()
        
        // Find all tsconfig.json files in the project
        val configFiles = FilenameIndex.getVirtualFilesByName(
            "tsconfig.json",
            GlobalSearchScope.projectScope(project)
        )
        
        for (configFile in configFiles) {
            try {
                val configInfo = parseTsConfig(configFile)
                if (configInfo != null) {
                    // Store by directory path for quick lookup
                    tsConfigs[configFile.parent.path] = configInfo
                    println("Found tsconfig.json: ${configFile.path}")
                    println("  baseUrl: ${configInfo.baseUrl}")
                    println("  paths: ${configInfo.paths?.keys}")
                    println("  outDir: ${configInfo.outDir}")
                }
            } catch (e: Exception) {
                println("Error parsing tsconfig.json at ${configFile.path}: ${e.message}")
            }
        }
        
        println("Total tsconfig.json files found: ${tsConfigs.size}")
    }
    
    private fun parseTsConfig(configFile: VirtualFile): TsConfigInfo? {
        try {
            val content = String(configFile.contentsToByteArray())
            
            // Parse JSON (handle comments by using a simple regex cleanup)
            val cleanedContent = removeJsonComments(content)
            val jsonObject = gson.fromJson(cleanedContent, JsonObject::class.java)
            
            val compilerOptions = jsonObject.getAsJsonObject("compilerOptions")
            
            val baseUrl = compilerOptions?.get("baseUrl")?.asString
            val outDir = compilerOptions?.get("outDir")?.asString
            val rootDir = compilerOptions?.get("rootDir")?.asString
            
            // Parse paths mapping
            val paths = compilerOptions?.get("paths")?.asJsonObject?.let { pathsObj ->
                pathsObj.entrySet().associate { (key, value) ->
                    key to value.asJsonArray.map { it.asString }
                }
            }
            
            return TsConfigInfo(
                configFile = configFile,
                baseUrl = baseUrl,
                paths = paths,
                outDir = outDir,
                rootDir = rootDir
            )
            
        } catch (e: JsonSyntaxException) {
            println("Invalid JSON in tsconfig.json: ${configFile.path}")
            return null
        } catch (e: Exception) {
            println("Error reading tsconfig.json: ${configFile.path} - ${e.message}")
            return null
        }
    }
    
    /**
     * Simple comment removal for JSON (not perfect but handles basic cases)
     */
    private fun removeJsonComments(json: String): String {
        return json.lines().joinToString("\n") { line ->
            val commentIndex = line.indexOf("//")
            if (commentIndex >= 0) line.substring(0, commentIndex) else line
        }
    }
    
    /**
     * Resolve a file path using TypeScript module resolution rules
     */
    fun resolveModulePath(sourceFile: VirtualFile, targetFile: VirtualFile): String {
        val tsConfig = getTsConfigForFile(sourceFile)
        val targetTsConfig = getTsConfigForFile(targetFile)
        val sameProject = tsConfig != null && tsConfig == targetTsConfig

        println("Resolving module path:")
        println("  Source: ${sourceFile.path}")
        println("  Target: ${targetFile.path}")
        println("  Same project: $sameProject")
        println("  TsConfig: ${tsConfig?.configFile?.path}")

        if (tsConfig != null) {
            println("  BaseUrl: ${tsConfig.baseUrl}")
            println("  RootDir: ${tsConfig.rootDir}")
            println("  Paths: ${tsConfig.paths?.keys}")

            // Try path mappings first (highest priority)
            resolveWithPathMappings(tsConfig, targetFile)?.let { return it }
            
            // Try baseUrl resolution for same-project files
            val targetTsConfig = getTsConfigForFile(targetFile)
            if (tsConfig == targetTsConfig) {
                resolveWithBaseUrl(tsConfig, targetFile)?.let { return it }
            }
        }
        
        // Fall back to relative path resolution
        return generateRelativePath(sourceFile, targetFile)
    }
    
    private fun resolveWithPathMappings(tsConfig: TsConfigInfo, targetFile: VirtualFile): String? {
        val configDir = tsConfig.configFile.parent
        
        tsConfig.paths?.forEach { (pattern, mappings) ->
            if (pattern.endsWith("/*")) {
                val prefix = pattern.substringBeforeLast("/*")
                mappings.forEach { mapping ->
                    if (mapping.endsWith("/*")) {
                        val mappingDir = mapping.substringBeforeLast("/*")
                        val actualDir = configDir.findFileByRelativePath(mappingDir)
                        
                        if (actualDir != null && targetFile.path.startsWith(actualDir.path)) {
                            val relativePart = targetFile.path.removePrefix(actualDir.path).removePrefix("/")
                            val modulePathWithoutExt = relativePart.substringBeforeLast(".")
                            return "$prefix/$modulePathWithoutExt"
                        }
                    }
                }
            }
        }
        return null
    }
    
    private fun resolveWithBaseUrl(tsConfig: TsConfigInfo, targetFile: VirtualFile): String? {
        if (tsConfig.baseUrl == null) return null
        
        val configDir = tsConfig.configFile.parent
        val baseDir = configDir.findFileByRelativePath(tsConfig.baseUrl) ?: return null
        
        // Try rootDir first, then baseUrl
        val resolveDirs = listOfNotNull(
            tsConfig.rootDir?.let { configDir.findFileByRelativePath("${tsConfig.baseUrl}/$it") },
            baseDir
        )
        
        for (resolveDir in resolveDirs) {
            if (targetFile.path.startsWith(resolveDir.path)) {
                val relativePath = try {
                    resolveDir.toNioPath().relativize(targetFile.toNioPath()).toString()
                } catch (e: Exception) {
                    continue
                }
                
                if (!relativePath.startsWith("..")) {
                    return relativePath.substringBeforeLast(".").replace("\\", "/")
                }
            }
        }
        return null
    }
    
    private fun generateRelativePath(fromFile: VirtualFile, toFile: VirtualFile): String {
        val fromDir = fromFile.parent
        
        val relativePath = try {
            fromDir.toNioPath().relativize(toFile.toNioPath()).toString()
        } catch (e: Exception) {
            toFile.name
        }
        
        // Remove file extension and ensure it starts with ./ for relative imports
        val pathWithoutExt = relativePath.substringBeforeLast(".")
        return if (pathWithoutExt.startsWith("..") || pathWithoutExt.startsWith("/")) {
            pathWithoutExt
        } else {
            "./$pathWithoutExt"
        }
    }
}