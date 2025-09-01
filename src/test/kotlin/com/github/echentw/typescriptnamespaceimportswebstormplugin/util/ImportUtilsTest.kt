package com.github.echentw.typescriptnamespaceimportswebstormplugin.util

import com.github.echentw.typescriptnamespaceimportswebstormplugin.settings.QuoteStyle
import org.junit.Test
import org.junit.Assert.*

class ImportUtilsTest {

    @Test
    fun testCreateImportStatementWithSingleQuotes() {
        val result = ImportUtils.createImportStatement("myModule", "path/to/module", QuoteStyle.SINGLE)
        assertEquals("import * as myModule from 'path/to/module';\n", result)
    }

    @Test
    fun testCreateImportStatementWithDoubleQuotes() {
        val result = ImportUtils.createImportStatement("myModule", "path/to/module", QuoteStyle.DOUBLE)
        assertEquals("import * as myModule from \"path/to/module\";\n", result)
    }

    @Test
    fun testHasExistingImportWithSingleQuotes() {
        val documentText = "import * as myModule from 'path/to/module';\nconst x = 1;"
        assertTrue(ImportUtils.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportWithDoubleQuotes() {
        val documentText = "import * as myModule from \"path/to/module\";\nconst x = 1;"
        assertTrue(ImportUtils.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportWithDifferentSpacing() {
        val documentText = "import  *   as   myModule   from   'path/to/module';\nconst x = 1;"
        assertTrue(ImportUtils.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportReturnsFalseWhenNotExists() {
        val documentText = "const x = 1;"
        assertFalse(ImportUtils.hasExistingImport(documentText, "myModule", "path/to/module"))
    }

    @Test
    fun testHasExistingImportWithSpecialCharactersInPath() {
        val documentText = "import * as myModule from 'path-with/special_chars.file';\n"
        assertTrue(ImportUtils.hasExistingImport(documentText, "myModule", "path-with/special_chars.file"))
    }

    @Test
    fun testParseImportStatementWithSingleQuotes() {
        val result = ImportUtils.parseImportStatement("import * as myModule from 'path/to/module';")
        assertNotNull(result)
        assertEquals("myModule", result?.moduleName)
        assertEquals("path/to/module", result?.importPath)
    }

    @Test
    fun testParseImportStatementWithDoubleQuotes() {
        val result = ImportUtils.parseImportStatement("import * as myModule from \"path/to/module\";")
        assertNotNull(result)
        assertEquals("myModule", result?.moduleName)
        assertEquals("path/to/module", result?.importPath)
    }

    @Test
    fun testParseImportStatementWithSpacing() {
        val result = ImportUtils.parseImportStatement("import  *  as  myModule  from  'path/to/module';")
        assertNotNull(result)
        assertEquals("myModule", result?.moduleName)
        assertEquals("path/to/module", result?.importPath)
    }

    @Test
    fun testParseImportStatementInvalid() {
        val result = ImportUtils.parseImportStatement("const x = 1;")
        assertNull(result)
    }

    @Test
    fun testParseImportStatementWithNewlineAndTrim() {
        val result = ImportUtils.parseImportStatement("  import * as myModule from 'path/to/module';\n  ")
        assertNotNull(result)
        assertEquals("myModule", result?.moduleName)
        assertEquals("path/to/module", result?.importPath)
    }
}