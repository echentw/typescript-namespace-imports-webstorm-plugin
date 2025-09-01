package com.github.echentw.typescriptnamespaceimportswebstormplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class PluginConfigurable : Configurable {
    
    private var settingsComponent: PluginSettingsComponent? = null

    override fun getDisplayName(): String = "TypeScript Namespace Imports"

    override fun getPreferredFocusedComponent(): JComponent? {
        return settingsComponent?.getPreferredFocusedComponent()
    }

    override fun createComponent(): JComponent? {
        settingsComponent = PluginSettingsComponent()
        return settingsComponent?.getPanel()
    }

    override fun isModified(): Boolean {
        val settings = PluginSettings.getInstance()
        return settingsComponent?.getQuoteStyle() != settings.quoteStyle
    }

    override fun apply() {
        val settings = PluginSettings.getInstance()
        settingsComponent?.getQuoteStyle()?.let { quoteStyle ->
            settings.quoteStyle = quoteStyle
        }
    }

    override fun reset() {
        val settings = PluginSettings.getInstance()
        settingsComponent?.setQuoteStyle(settings.quoteStyle)
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}

class PluginSettingsComponent {
    
    private val panel: JPanel
    private val quoteStyleComboBox = ComboBox(QuoteStyle.values())

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Quote style for imports:"), quoteStyleComboBox, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        // Set custom renderer for better display
        quoteStyleComboBox.renderer = QuoteStyleListCellRenderer()
    }

    fun getPanel(): JPanel = panel

    fun getPreferredFocusedComponent(): JComponent = quoteStyleComboBox

    fun getQuoteStyle(): QuoteStyle = quoteStyleComboBox.selectedItem as QuoteStyle

    fun setQuoteStyle(quoteStyle: QuoteStyle) {
        quoteStyleComboBox.selectedItem = quoteStyle
    }
}