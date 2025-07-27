package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TypeScriptFileScannerService
import java.io.File

class BasicCompletionContributor : CompletionContributor() {

    init {
        // Register our completion provider for any text
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.position.project
                    val fileScanner = project.getService(TypeScriptFileScannerService::class.java)
                    val modulesByPrefix = fileScanner.getModulesByFirstLetter()

                    // Get the prefix being typed
                    val prefix = result.prefixMatcher.prefix.lowercase()

                    // Still keep the original "asdf" test for now
                    if ("asdf".startsWith(prefix)) {
                        result.addElement(
                            LookupElementBuilder.create("asdf!!!")
                                .withPresentableText("asdf!!!")
                                .withTailText(" (Basic completion test)")
                        )
                    }

                    // Check if prefix is empty to avoid issues
                    if (prefix.isEmpty()) {
                        return
                    }
                    
                    val letter = prefix.substring(0, 1)

                    // Add completion suggestions based on module names
                    // Use originalFile.virtualFile which works reliably in completion context
                    val currentFile = parameters.originalFile?.virtualFile ?: parameters.position.containingFile?.virtualFile
                    
                    if (currentFile != null) {
                        modulesByPrefix[letter]?.forEach { moduleInfo ->
                            if (moduleInfo.moduleName.lowercase().startsWith(prefix)) {
                                // Generate relative import path
                                val relativePath = generateRelativePath(project, currentFile, moduleInfo.virtualFile)
                                val importStatement = "import * as ${moduleInfo.moduleName} from '$relativePath'"

                                result.addElement(
                                    LookupElementBuilder.create(importStatement)
                                        .withPresentableText(moduleInfo.moduleName)
                                        .withTailText(" from $relativePath")
                                        .withTypeText("namespace import")
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    private fun generateRelativePath(project: Project, fromFile: VirtualFile, toFile: VirtualFile): String {
        val projectBasePath = project.basePath ?: return toFile.path

        // Get the directory of the current file
        val fromDir = fromFile.parent

        // Calculate relative path from current file's directory to target file
        val fromPath = File(fromDir.path)
        val toPath = File(toFile.path)

        val relativePath = try {
            fromPath.toPath().relativize(toPath.toPath()).toString()
        } catch (e: Exception) {
            // Fallback to absolute path relative to project root
            toFile.path.removePrefix(projectBasePath).removePrefix("/")
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