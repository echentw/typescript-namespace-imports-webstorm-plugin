package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TypeScriptFileScannerService
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TsConfigService

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
                    val tsConfigService = project.getService(TsConfigService::class.java)
                    val modulesByPrefix = fileScanner.getModulesByFirstLetter()

                    // Get the prefix being typed
                    val prefix = result.prefixMatcher.prefix.lowercase()


                    // Check if prefix is empty to avoid issues
                    if (prefix.isEmpty()) {
                        return
                    }
                    
                    val letter = prefix.substring(0, 1)

                    // Add completion suggestions based on module names
                    // Use originalFile.virtualFile which works reliably in completion context
                    val currentFile = parameters.originalFile.virtualFile
                    
                    if (currentFile != null) {
                        modulesByPrefix[letter]?.forEach { moduleInfo ->
                            if (moduleInfo.moduleName.lowercase().startsWith(prefix)) {
                                // Use TsConfigService for proper path resolution
                                val modulePath = tsConfigService.resolveModulePath(currentFile, moduleInfo.virtualFile)
                                val importStatement = "import * as ${moduleInfo.moduleName} from '$modulePath';"

                                result.addElement(
                                    LookupElementBuilder.create(importStatement)
                                        .withPresentableText(moduleInfo.moduleName)
                                        .withTailText(" from $modulePath")
                                        .withTypeText("namespace import")
                                )
                            }
                        }
                    }
                }
            }
        )
    }

}