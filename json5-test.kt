import de.marhali.json5.Json5
import com.google.gson.Gson
import com.google.gson.JsonObject

fun main() {
    // Sample JSON5 content with comments and trailing commas
    val json5Content = """
    {
        // This is a comment
        "compilerOptions": {
            "baseUrl": "./src",
            "paths": {
                "@components/*": ["components/*"],
                "@utils/*": ["utils/*"],
            }, // trailing comma
            "outDir": "./dist"
        }
    }
    """
    
    try {
        // Create Json5 instance with appropriate options
        val json5Instance = Json5.builder { options ->
            options.allowInvalidSurrogate()
                .quoteSingle()
                .trailingComma()
                .build()
        }
        
        // Parse JSON5 content
        val json5Element = json5Instance.parse(json5Content)
        
        // Convert to JSON string for Gson compatibility
        val jsonString = json5Element.toString()
        println("Parsed JSON: $jsonString")
        
        // Use with Gson
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
        
        val compilerOptions = jsonObject.getAsJsonObject("compilerOptions")
        val baseUrl = compilerOptions?.get("baseUrl")?.asString
        val outDir = compilerOptions?.get("outDir")?.asString
        
        println("BaseUrl: $baseUrl")
        println("OutDir: $outDir")
        
        // Access paths
        val paths = compilerOptions?.get("paths")?.asJsonObject
        paths?.entrySet()?.forEach { (key, value) ->
            println("Path mapping: $key -> ${value.asJsonArray.map { it.asString }}")
        }
        
    } catch (e: Exception) {
        println("Error parsing JSON5: ${e.message}")
    }
}