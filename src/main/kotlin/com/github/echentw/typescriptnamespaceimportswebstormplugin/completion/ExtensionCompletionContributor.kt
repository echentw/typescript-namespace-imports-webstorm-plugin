package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.ExtensionServiceImpl
import com.github.echentw.typescriptnamespaceimportswebstormplugin.settings.PluginSettings
import com.github.echentw.typescriptnamespaceimportswebstormplugin.util.ImportUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager

class ExtensionCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            ExtensionCompletionProvider(),
        )
    }
}

class ExtensionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val service = project.getService(ExtensionServiceImpl::class.java)

        // Call initialize here again, just in case
        service.initialize()

        val currentFile = parameters.originalFile.virtualFile
        if (currentFile == null) return

        val query = result.prefixMatcher.prefix.lowercase()
        if (query.isEmpty()) return

        val modules = service.getModulesForCompletion(currentFile, query)
        val settings = PluginSettings.getInstance()
        val documentText = parameters.originalFile.fileDocument.text
        
        for (module in modules) {
            if (ImportUtil.hasExistingImport(documentText, module.moduleName, module.importPath)) {
                continue
            }
            
            val importStatement = ImportUtil.makeImportStatement(
                module.moduleName, 
                module.importPath, 
                settings.quoteStyle
            )
            
            result.addElement(
                LookupElementBuilder.create(module.moduleName)
                    .withPresentableText(module.moduleName)
                    .withTailText(" from ${module.importPath}")
                    .withTypeText("namespace import")
                    .withInsertHandler(InsertImportStatementHandler(importStatement))
            )
        }
    }
}

class InsertImportStatementHandler(private val importStatement: String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val project = context.project
        val psiFile = context.file
        
        // The module name will already be automatically inserted.
        // We just need to add the import statement to the top of the file.
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(0, importStatement)
            PsiDocumentManager.getInstance(psiFile.project).commitDocument(document)
        }
    }
}
