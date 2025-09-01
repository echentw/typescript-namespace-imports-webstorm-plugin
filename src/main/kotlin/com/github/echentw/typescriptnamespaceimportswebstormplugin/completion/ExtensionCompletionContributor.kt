package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.ExtensionServiceImpl
import com.github.echentw.typescriptnamespaceimportswebstormplugin.settings.PluginSettings
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
        val quoteChar = settings.quoteStyle.character
        
        for (module in modules) {
            val importStatement = "import * as ${module.moduleName} from ${quoteChar}${module.importPath}${quoteChar};\n"
            
            // Check if import already exists with either quote style
            val documentText = parameters.originalFile.fileDocument.text
            val singleQuoteImport = "import * as ${module.moduleName} from '${module.importPath}'"
            val doubleQuoteImport = "import * as ${module.moduleName} from \"${module.importPath}\""
            
            if (!documentText.contains(singleQuoteImport) && !documentText.contains(doubleQuoteImport)) {
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
}

class InsertImportStatementHandler(private val importStatement: String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val project = context.project
        val psiFile = context.file
        
        // The module name will already be automatically inserted.
        // We just need to add the import statement to the top of the file.
        WriteCommandAction.runWriteCommandAction(project, fun() {
            // Extract module name and path for duplicate checking
            val importRegex = Regex("import \\* as (\\w+) from ['\"]([^'\"]+)['\"];")
            val match = importRegex.find(importStatement.trim())
            
            if (match != null) {
                val moduleName = match.groupValues[1]
                val importPath = match.groupValues[2]
                
                // Check if import already exists with either quote style
                val documentText = document.text
                val singleQuoteImport = "import * as $moduleName from '$importPath'"
                val doubleQuoteImport = "import * as $moduleName from \"$importPath\""
                
                if (!documentText.contains(singleQuoteImport) && !documentText.contains(doubleQuoteImport)) {
                    document.insertString(0, importStatement)
                    PsiDocumentManager.getInstance(psiFile.project).commitDocument(document)
                }
            }
        })
    }
}
