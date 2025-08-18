package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.NamespaceImportCompletionServiceImpl
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.Document
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
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

                    val asdf = project.getService(NamespaceImportCompletionServiceImpl::class.java)
                    asdf.initialize()

                    // Ensure services are initialized (fallback for safety)
                    fileScanner.initialize()
                    tsConfigService.initialize()
                    
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

                                result.addElement(
                                    LookupElementBuilder.create(moduleInfo.moduleName)
                                        .withPresentableText(moduleInfo.moduleName)
                                        .withTailText(" from $modulePath")
                                        .withTypeText("namespace import")
                                        .withInsertHandler(ImportInsertHandler(moduleInfo.moduleName, modulePath))
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

class ImportInsertHandler(
    private val moduleName: String,
    private val modulePath: String
) : InsertHandler<LookupElement> {
    
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val project = context.project
        val psiFile = context.file
        
        // Insert the module name at cursor (this happens automatically by the LookupElement)
        // Now we need to add the import statement at the top
        
        WriteCommandAction.runWriteCommandAction(project) {
            addImportStatement(document, psiFile, moduleName, modulePath)
        }
    }
    
    private fun addImportStatement(document: Document, psiFile: PsiFile, moduleName: String, modulePath: String) {
        val importStatement = "import * as $moduleName from '$modulePath';\n"
        
        // Check if import already exists
        if (document.text.contains(importStatement)) {
            return // Import already exists
        }
        
        // Insert the import statement
        document.insertString(0, importStatement)
        
        // Commit the document changes
        PsiDocumentManager.getInstance(psiFile.project).commitDocument(document)
    }
}