package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

interface ExtensionService {
    fun initialize()
    fun getModulesForCompletion(file: VirtualFile, query: String): List<ModuleForCompletion>
}

@Service(Service.Level.PROJECT)
class ExtensionServiceImpl(private val project: Project) : ExtensionService {

    private var hasInitStarted = false
    private var service: NamespaceImportService? = null

    override fun initialize() {
        if (!hasInitStarted) {
            hasInitStarted = true

            val service = NamespaceImportServiceImpl(project)
            setupFileWatcher(service)
            this.service = service
        }
    }

    override fun getModulesForCompletion(file: VirtualFile, query: String): List<ModuleForCompletion> {
        if (service == null) return emptyList()
        return service!!.getModulesForCompletion(file, query)
    }

    private fun setupFileWatcher(service: NamespaceImportService) {
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) {
                    when (event) {
                        is VFileCreateEvent -> {
                            println("[VFileCreateEvent]: ${event.file}")
                            val file = event.file
                            if (file != null) {
                                service.handleFileCreated(file)
                            }
                        }
                        is VFileDeleteEvent -> {
                            println("[VFileDeleteEvent]: ${event.file}")
                            service.handleFileDeleted(event.file)
                        }
                        is VFileMoveEvent -> {
                            println("[VFileMoveEvent]: ${event.file}")
                            service.handleFileDeleted(event.file)
                            service.handleFileCreated(event.file)
                        }
                        is VFilePropertyChangeEvent -> {
                            println("[VFilePropertyChangeEvent]: ${event.file}")
                            if (event.propertyName == VirtualFile.PROP_NAME) {
                                // File was renamed
                                service.handleFileDeleted(event.file)
                                service.handleFileCreated(event.file)
                            }
                        }
                        is VFileContentChangeEvent -> {
                            println("[VFileChangeEvent]: ${event.file}")
                            service.handleFileContentChanged(event.file)
                        }
                    }
                }
            }
        })
    }
}
