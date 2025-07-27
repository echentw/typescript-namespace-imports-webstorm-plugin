package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import java.util.concurrent.ConcurrentHashMap

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
    
    init {
        scanForTsConfigs()
    }
    
    fun getAllTsConfigs(): List<TsConfigInfo> {
        return tsConfigs.values.toList()
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
        val configFiles = FilenameIndex.getFilesByName(
            project,
            "tsconfig.json",
            GlobalSearchScope.projectScope(project)
        )
        
        for (psiFile in configFiles) {
            val configFile = psiFile.virtualFile
            if (configFile != null) {
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
        return json.lines()
            .map { line ->
                // Remove single-line comments
                val commentIndex = line.indexOf("//")
                if (commentIndex >= 0) {
                    line.substring(0, commentIndex)
                } else {
                    line
                }
            }
            .joinToString("\n")
    }
    
    /**
     * Resolve a file path using TypeScript module resolution rules
     */
    fun resolveModulePath(sourceFile: VirtualFile, targetFile: VirtualFile): String {
        val tsConfig = getTsConfigForFile(sourceFile)
        val targetTsConfig = getTsConfigForFile(targetFile)
        
        // Check if both files are in the same TypeScript project
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
        }
        
        if (tsConfig != null) {
            val configDir = tsConfig.configFile.parent
            
            // First, check if any path mappings apply
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
                                val resolvedPath = "$prefix/$modulePathWithoutExt"
                                println("  Resolved path: $resolvedPath (path mapping)")
                                return resolvedPath
                            }
                        }
                    }
                }
            }
            
            // For same-project files, use baseUrl + rootDir resolution
            if (sameProject && tsConfig.baseUrl != null) {
                val baseDir = configDir.findFileByRelativePath(tsConfig.baseUrl)
                
                if (baseDir != null) {
                    // If rootDir is specified, files should be resolved relative to baseUrl+rootDir
                    val resolveFromDir = if (tsConfig.rootDir != null) {
                        configDir.findFileByRelativePath("${tsConfig.baseUrl}/${tsConfig.rootDir}")
                            ?: baseDir
                    } else {
                        baseDir
                    }
                    
                    // Check if target file is within the resolve directory
                    if (targetFile.path.startsWith(resolveFromDir.path)) {
                        val relativePath = try {
                            resolveFromDir.toNioPath().relativize(targetFile.toNioPath()).toString()
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (relativePath != null && !relativePath.startsWith("..")) {
                            // Remove file extension for TypeScript imports
                            val modulePathWithoutExt = relativePath.substringBeforeLast(".")
                            val resolvedPath = modulePathWithoutExt.replace("\\", "/") // Normalize path separators
                            println("  Resolved path: $resolvedPath (baseUrl+rootDir)")
                            return resolvedPath
                        }
                    }
                    
                    // If not in rootDir, try baseUrl resolution
                    val relativePath = try {
                        baseDir.toNioPath().relativize(targetFile.toNioPath()).toString()
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (relativePath != null && !relativePath.startsWith("..")) {
                        // Remove file extension for TypeScript imports
                        val modulePathWithoutExt = relativePath.substringBeforeLast(".")
                        val resolvedPath = modulePathWithoutExt.replace("\\", "/") // Normalize path separators
                        println("  Resolved path: $resolvedPath (baseUrl)")
                        return resolvedPath
                    }
                }
            }
        }
        
        // Fall back to relative path resolution for cross-project or when no tsconfig
        val fallbackPath = generateRelativePath(sourceFile, targetFile)
        println("  Resolved path: $fallbackPath (fallback)")
        return fallbackPath
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