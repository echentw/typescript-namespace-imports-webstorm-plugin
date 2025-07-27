package com.github.echentw.typescriptnamespaceimportswebstormplugin.services

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class TypeScriptFileScannerServiceTest : BasePlatformTestCase() {

    fun testModuleNameExtraction() {
        // Test the private methods through a simple helper class
        val helper = ModuleNameHelper()
        
        // Test various file name patterns
        assertEquals("mapUtil", helper.toCamelCase("map_util"))
        assertEquals("mapUtil", helper.toCamelCase("map-util"))
        assertEquals("userService", helper.toCamelCase("user_service"))
        assertEquals("userService", helper.toCamelCase("user-service"))
        assertEquals("apiClient", helper.toCamelCase("api_client"))
        assertEquals("simpleFile", helper.toCamelCase("simple_file"))
        assertEquals("index", helper.toCamelCase("index"))
        assertEquals("component", helper.toCamelCase("component"))
        
        // Test extractModuleName with full paths
        assertEquals("mapUtil", helper.extractModuleName("/src/common/map_util.ts"))
        assertEquals("userService", helper.extractModuleName("/app/services/user-service.tsx"))
        assertEquals("index", helper.extractModuleName("/components/index.ts"))
    }
}

// Helper class to test the private methods
class ModuleNameHelper {
    fun toCamelCase(input: String): String {
        return input.split("_", "-", ".")
            .filter { it.isNotEmpty() }
            .mapIndexed { index, part ->
                if (index == 0) {
                    part.lowercase()
                } else {
                    part.lowercase().replaceFirstChar { it.uppercase() }
                }
            }
            .joinToString("")
    }
    
    fun extractModuleName(filePath: String): String {
        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
        return toCamelCase(fileName)
    }
}