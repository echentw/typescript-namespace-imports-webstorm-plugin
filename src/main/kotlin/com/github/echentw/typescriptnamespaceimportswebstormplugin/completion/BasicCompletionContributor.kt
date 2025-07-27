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
                    // Get the prefix being typed
                    val prefix = result.prefixMatcher.prefix
                    
                    // Check if the prefix is "asdf"
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