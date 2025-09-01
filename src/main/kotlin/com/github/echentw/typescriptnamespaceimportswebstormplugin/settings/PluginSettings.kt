package com.github.echentw.typescriptnamespaceimportswebstormplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

enum class QuoteStyle(val displayName: String, val character: Char) {
    SINGLE("Single quotes (')", '\''),
    DOUBLE("Double quotes (\")", '"')
}

@State(
    name = "TypescriptNamespaceImportsSettings",
    storages = [Storage("typescript-namespace-imports.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings> {
    
    var quoteStyle: QuoteStyle = QuoteStyle.SINGLE

    override fun getState(): PluginSettings = this

    override fun loadState(state: PluginSettings) {
        try {
            XmlSerializerUtil.copyBean(state, this)
        } catch (e: Exception) {
            // If settings can't be loaded, keep defaults
            // Log the error but don't crash the plugin
            println("TypeScript Namespace Imports: Failed to load settings, using defaults: ${e.message}")
        }
    }

    companion object {
        fun getInstance(): PluginSettings {
            return try {
                ApplicationManager.getApplication().getService(PluginSettings::class.java)
            } catch (e: Exception) {
                // Fallback to default settings if service can't be obtained
                println("TypeScript Namespace Imports: Failed to get settings service, using defaults: ${e.message}")
                PluginSettings()
            }
        }
    }
}