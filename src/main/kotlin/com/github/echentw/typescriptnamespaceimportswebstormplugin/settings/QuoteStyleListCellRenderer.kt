package com.github.echentw.typescriptnamespaceimportswebstormplugin.settings

import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class QuoteStyleListCellRenderer : DefaultListCellRenderer() {
    
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        
        if (value is QuoteStyle) {
            text = value.displayName
        }
        
        return component
    }
}