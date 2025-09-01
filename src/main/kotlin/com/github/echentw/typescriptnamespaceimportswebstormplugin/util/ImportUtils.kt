package com.github.echentw.typescriptnamespaceimportswebstormplugin.util

import com.github.echentw.typescriptnamespaceimportswebstormplugin.settings.QuoteStyle

object ImportUtils {
    
    /**
     * Generates an import statement with the specified quote style
     */
    fun createImportStatement(moduleName: String, importPath: String, quoteStyle: QuoteStyle): String {
        val quote = quoteStyle.character
        return "import * as $moduleName from $quote$importPath$quote;\n"
    }
    
    /**
     * Checks if an import for the given module and path already exists in the document,
     * regardless of quote style
     */
    fun hasExistingImport(documentText: String, moduleName: String, importPath: String): Boolean {
        // Use regex to match import with either quote style
        val pattern = "import\\s*\\*\\s*as\\s+$moduleName\\s+from\\s*['\"]${Regex.escape(importPath)}['\"]"
        return Regex(pattern).containsMatchIn(documentText)
    }
    
    /**
     * Data class representing a parsed import statement
     */
    data class ParsedImport(val moduleName: String, val importPath: String)
    
    /**
     * Parses an import statement to extract module name and path
     */
    fun parseImportStatement(importStatement: String): ParsedImport? {
        val pattern = """import\s*\*\s*as\s+(\w+)\s+from\s*['"]([^'"]+)['"]""".toRegex()
        val match = pattern.find(importStatement.trim())
        return match?.let { 
            ParsedImport(it.groupValues[1], it.groupValues[2])
        }
    }
}