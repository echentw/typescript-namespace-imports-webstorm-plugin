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
                    val tsFileByPath = fileScanner.getTsFileByPath()
                    
                    // Get the prefix being typed
                    val prefix = result.prefixMatcher.prefix
                    
                    println("Completion triggered with prefix: '$prefix'")
                    println("Found ${tsFileByPath.size} TypeScript files")

                    // Still keep the original "asdf" test for now
                    if ("asdf".startsWith(prefix)) {
                        result.addElement(
                            LookupElementBuilder.create("asdf!!!")
                                .withPresentableText("asdf!!!")
                                .withTailText(" (Basic completion test)")
                        )
                    }
                    
                    // Add completion for TypeScript files (for testing)
                    for (entry in tsFileByPath) {
                        if (entry.value.name.startsWith(prefix)) {
                            result.addElement(
                                LookupElementBuilder.create(entry.value.name)
                                    .withPresentableText(entry.value.name)
                                    .withTailText(" (Found ${tsFileByPath.size} TS files)")
                            )
                        }
                    }
                }
            }
        )
    }
}