package com.github.echentw.typescriptnamespaceimportswebstormplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class PluginConfigurable : Configurable {
    
    private var quoteStyleComboBox: ComboBox<QuoteStyle>? = null
    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "TypeScript Namespace Imports"

    override fun getPreferredFocusedComponent(): JComponent? = quoteStyleComboBox

    override fun createComponent(): JComponent? {
        quoteStyleComboBox = ComboBox(QuoteStyle.entries.toTypedArray()).apply {
            // Use built-in toString() from enum display name
            renderer = object : javax.swing.DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: javax.swing.JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ) = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).apply {
                    text = (value as? QuoteStyle)?.displayName ?: value?.toString()
                }
            }
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Quote style for imports:"), quoteStyleComboBox!!, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel
    }

    override fun isModified(): Boolean {
        val settings = PluginSettings.getInstance()
        return quoteStyleComboBox?.selectedItem != settings.quoteStyle
    }

    override fun apply() {
        val settings = PluginSettings.getInstance()
        val selectedItem = quoteStyleComboBox?.selectedItem
        if (selectedItem is QuoteStyle) {
            settings.quoteStyle = selectedItem
        }
    }

    override fun reset() {
        val settings = PluginSettings.getInstance()
        quoteStyleComboBox?.selectedItem = settings.quoteStyle
    }

    override fun disposeUIResources() {
        quoteStyleComboBox = null
        mainPanel = null
    }
}
