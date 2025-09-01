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
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): PluginSettings {
            return ApplicationManager.getApplication().getService(PluginSettings::class.java)
        }
    }
}