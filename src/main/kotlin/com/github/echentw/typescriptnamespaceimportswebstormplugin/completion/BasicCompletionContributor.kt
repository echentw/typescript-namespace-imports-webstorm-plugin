package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TypeScriptFileScannerService

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
                    val modulesByPrefix = fileScanner.getModulesByPrefix()
                    
                    // Get the prefix being typed
                    val prefix = result.prefixMatcher.prefix.lowercase()
                    
                    println("Completion triggered with prefix: '$prefix'")
                    println("Total module prefixes available: ${modulesByPrefix.size}")

                    // Still keep the original "asdf" test for now
                    if ("asdf".startsWith(prefix)) {
                        result.addElement(
                            LookupElementBuilder.create("asdf!!!")
                                .withPresentableText("asdf!!!")
                                .withTailText(" (Basic completion test)")
                        )
                    }

                    // Add completion suggestions based on module names
                    // Iterate through all modules and check if any start with the prefix
                    modulesByPrefix.values.flatten().forEach { moduleInfo ->
                        if (moduleInfo.moduleName.lowercase().startsWith(prefix)) {
                            // Generate relative import path (for now using full path)
                            val relativePath = generateRelativePath(moduleInfo.filePath)
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
        )
    }
    
    private fun generateRelativePath(filePath: String): String {
        // For now, just use the file path without extension
        // TODO: This should be made relative to the current file and respect tsconfig paths
        return filePath.substringBeforeLast(".").replace("/Users/ericchen/Documents/github/typescript-namespace-imports-webstorm-plugin/", "./")
    }
}