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
                    modulesByPrefix[prefix.substring(0, 1)]?.forEach { moduleInfo ->
                        val importStatement = "import * as ${moduleInfo.moduleName} from '${moduleInfo.filePath}'"
                        result.addElement(
                            LookupElementBuilder.create(importStatement)
                                .withPresentableText(moduleInfo.moduleName)
                                .withTailText(" from ${moduleInfo.filePath}")
                                .withTypeText("namespace import")
                        )
                    }
                }
            }
        )
    }
}