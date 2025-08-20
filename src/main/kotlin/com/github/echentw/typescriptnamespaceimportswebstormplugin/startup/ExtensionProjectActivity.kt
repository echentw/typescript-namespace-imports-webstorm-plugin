package com.github.echentw.typescriptnamespaceimportswebstormplugin.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.application.ApplicationManager
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.ExtensionService

class ExtensionProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().info("TypeScript Namespace Imports plugin initializing for project: ${project.name}")
        
        // Initialize services in background to avoid blocking UI
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                try {
                    val service = project.getService(ExtensionService::class.java)
                    service.initialize()

                    thisLogger().info("TypeScript Namespace Imports plugin initialization complete for project: ${project.name}")
                } catch (e: Exception) {
                    thisLogger().error("Failed to initialize TypeScript Namespace Imports plugin", e)
                }
            }
        }
    }
}
