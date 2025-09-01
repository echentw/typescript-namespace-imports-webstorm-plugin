package com.github.echentw.typescriptnamespaceimportswebstormplugin.settings

import org.junit.Test
import org.junit.Assert.*

class PluginSettingsTest {

    @Test
    fun testQuoteStyleEnum() {
        // Test that QuoteStyle enum has correct values
        assertEquals("Single quotes (')", QuoteStyle.SINGLE.displayName)
        assertEquals("Double quotes (\")", QuoteStyle.DOUBLE.displayName)
        assertEquals('\'', QuoteStyle.SINGLE.character)
        assertEquals('"', QuoteStyle.DOUBLE.character)
    }

    @Test
    fun testDefaultSettings() {
        // Test that default settings use single quotes
        val settings = PluginSettings()
        assertEquals(QuoteStyle.SINGLE, settings.quoteStyle)
    }

    @Test
    fun testSettingsModification() {
        // Test that settings can be modified
        val settings = PluginSettings()
        settings.quoteStyle = QuoteStyle.DOUBLE
        assertEquals(QuoteStyle.DOUBLE, settings.quoteStyle)
        
        settings.quoteStyle = QuoteStyle.SINGLE
        assertEquals(QuoteStyle.SINGLE, settings.quoteStyle)
    }
}