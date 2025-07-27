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
                    // Get the text before the cursor
                    val elementAtCaret = parameters.position
                    val text = elementAtCaret.text
                    
                    // Check if the current text contains "asdf"
                    if (text.contains("asdf")) {
                        // Add a completion suggestion
                        val lookupElement = LookupElementBuilder.create("asdf!")
                            .withPresentableText("asdf!")
                            .withTailText(" (Basic completion test)")
                            .withInsertHandler { context, _ ->
                                // Replace "asdf" with "asdf!"
                                val document = context.document
                                val startOffset = context.startOffset
                                val currentText = document.getText()
                                val lineStart = currentText.lastIndexOf('\n', startOffset) + 1
                                val lineEnd = currentText.indexOf('\n', startOffset).let { 
                                    if (it == -1) currentText.length else it 
                                }
                                val lineText = currentText.substring(lineStart, lineEnd)
                                
                                val asdfIndex = lineText.indexOf("asdf")
                                if (asdfIndex != -1) {
                                    val replaceStart = lineStart + asdfIndex
                                    val replaceEnd = replaceStart + 4 // length of "asdf"
                                    document.replaceString(replaceStart, replaceEnd, "asdf!")
                                }
                            }
                        
                        result.addElement(lookupElement)
                    }
                }
            }
        )
    }
}