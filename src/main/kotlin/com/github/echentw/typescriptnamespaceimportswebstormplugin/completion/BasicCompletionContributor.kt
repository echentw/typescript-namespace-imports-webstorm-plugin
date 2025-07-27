package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

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
                    // Always add a test completion (for debugging)
                    result.addElement(
                        LookupElementBuilder.create("DEBUG_TEST")
                            .withPresentableText("DEBUG_TEST")
                            .withTailText(" (Plugin is working!)")
                    )
                    
                    // Get the prefix being typed
                    val prefix = result.prefixMatcher.prefix
                    
                    // Check if the prefix contains "asdf"
                    if (prefix.contains("asdf")) {
                        result.addElement(
                            LookupElementBuilder.create("asdf!")
                                .withPresentableText("asdf!")
                                .withTailText(" (Basic completion test)")
                        )
                    }
                }
            }
        )
    }
}