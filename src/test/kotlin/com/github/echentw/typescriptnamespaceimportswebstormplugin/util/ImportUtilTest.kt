package com.github.echentw.typescriptnamespaceimportswebstormplugin.util

import com.github.echentw.typescriptnamespaceimportswebstormplugin.settings.QuoteStyle
import org.junit.Test
import org.junit.Assert.*

class ImportUtilTest {

    @Test
    fun testCreateImportStatementWithSingleQuotes() {
        val result = ImportUtil.createImportStatement("myModule", "path/to/module", QuoteStyle.SINGLE)
        assertEquals("import * as myModule from 'path/to/module';\n", result)
    }

    @Test
    fun testCreateImportStatementWithDoubleQuotes() {
        val result = ImportUtil.createImportStatement("myModule", "path/to/module", QuoteStyle.DOUBLE)
        assertEquals("import * as myModule from \"path/to/module\";\n", result)
    }

    @Test
    fun testHasExistingImportWithSingleQuotes() {
        val documentText = "import * as myModule from 'path/to/module';\nconst x = 1;"
        assertTrue(ImportUtil.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportWithDoubleQuotes() {
        val documentText = "import * as myModule from \"path/to/module\";\nconst x = 1;"
        assertTrue(ImportUtil.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportWithDifferentSpacing() {
        val documentText = "import  *   as   myModule   from   'path/to/module';\nconst x = 1;"
        assertTrue(ImportUtil.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportReturnsFalseWhenNotExists() {
        val documentText = "const x = 1;"
        assertFalse(ImportUtil.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportWithSpecialCharactersInPath() {
        val documentText = "import * as myModule from 'path-with/special_chars.file';\n"
        assertTrue(ImportUtil.hasExistingImport(documentText, "myModule", "path-with/special_chars.file"))
    }

}