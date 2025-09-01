package com.github.echentw.typescriptnamespaceimportswebstormplugin.util

import com.github.echentw.typescriptnamespaceimportswebstormplugin.settings.QuoteStyle

object ImportUtil {
    
    fun createImportStatement(moduleName: String, importPath: String, quoteStyle: QuoteStyle): String {
        val quote = quoteStyle.character
        return "import * as $moduleName from $quote$importPath$quote;\n"
    }
    
    fun hasExistingImport(documentText: String, moduleName: String, importPath: String): Boolean {
        // Use regex to match import with either quote style
        val pattern = "import\\s*\\*\\s*as\\s+$moduleName\\s+from\\s*['\"]${Regex.escape(importPath)}['\"]"
        return Regex(pattern).containsMatchIn(documentText)
    }
    
}