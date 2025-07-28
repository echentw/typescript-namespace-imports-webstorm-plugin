package com.github.echentw.typescriptnamespaceimportswebstormplugin.completion

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
                                // Check if this file should be accessible from the current file's project context
                                if (shouldIncludeFileInCompletion(currentFile, moduleInfo.virtualFile, tsConfigService)) {
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

/**
 * Determines if a target file should be included in completion suggestions from the current file.
 * This enforces rootDir restrictions - only files under the current project's rootDir are included.
 */
private fun shouldIncludeFileInCompletion(
    currentFile: VirtualFile,
    targetFile: VirtualFile,
    tsConfigService: TsConfigService
): Boolean {
    val currentTsConfig = tsConfigService.getTsConfigForFile(currentFile)
    val targetTsConfig = tsConfigService.getTsConfigForFile(targetFile)
    
    // If current file has no tsconfig, include all files (fallback behavior)
    if (currentTsConfig == null) {
        return true
    }
    
    // If target file has no tsconfig, exclude it (it's not part of any TS project)
    if (targetTsConfig == null) {
        return false
    }
    
    // If both files are in the same TS project, they should already be filtered correctly
    // by the scanner's shouldIgnoreFileBasedOnTsConfig logic
    if (currentTsConfig == targetTsConfig) {
        return true
    }
    
    // Files from different TS projects: apply rootDir check for the target file
    if (targetTsConfig.rootDir != null) {
        val targetConfigDir = targetTsConfig.configFile.parent
        val targetRootDir = targetConfigDir.findFileByRelativePath(targetTsConfig.rootDir)
        
        // Only include target file if it's under its project's rootDir
        if (targetRootDir != null && !targetFile.path.startsWith(targetRootDir.path)) {
            return false
        }
    }
    
    return true
}