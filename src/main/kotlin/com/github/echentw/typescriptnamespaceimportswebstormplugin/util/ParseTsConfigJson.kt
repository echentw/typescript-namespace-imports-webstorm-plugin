import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TsConfigJson
import com.intellij.openapi.vfs.VirtualFile
import de.marhali.json5.Json5
import de.marhali.json5.Json5Array
import de.marhali.json5.Json5Element
import de.marhali.json5.Json5Object
import de.marhali.json5.exception.Json5Exception

fun parseTsConfigJson(file: VirtualFile): Result<TsConfigJson, String> {
    val content: String
    try {
        content = String(file.contentsToByteArray())
    } catch (e: Exception) {
        return Result.err("Failed to read ${file.path}: ${e.toString()}")
    }

    val json5Instance = Json5.builder { options ->
        options.allowInvalidSurrogate()
            .quoteSingle() // does this reject double quotes?
            .trailingComma() // does this reject lines without a trailing comma?
            .build()
    }

    val parseErrorPrefix = "Failed to parse ${file.path}"

    val tsConfig: Json5Object
    try {
        tsConfig = json5Instance.parse(content).asJson5Object
            ?: return Result.err("$parseErrorPrefix: contents is not object")
    } catch (e: Json5Exception) {
        return Result.err("$parseErrorPrefix: ${e.toString()}")
    }

    val compilerOptions: Json5Object
    try {
        val element: Json5Element = tsConfig.get("compilerOptions")
            ?: return Result.ok(TsConfigJson(null, null, null))

        compilerOptions = element.asJson5Object
            ?: return Result.err("$parseErrorPrefix: \"compilerOptions\" is not an object")

    } catch (e: Json5Exception) {
        return Result.err("$parseErrorPrefix: ${e.toString()}")
    }

    val baseUrl: String?
    try {
        val element: Json5Element? = compilerOptions.get("baseUrl")
        baseUrl = if (element == null) {
            null
        } else {
            element.asString
                ?: return Result.err("$parseErrorPrefix: \"baseUrl\" is not a string")
        }
    } catch (e: Json5Exception) {
        return Result.err("$parseErrorPrefix: ${e.toString()}")
    }

    val paths: Map<String, List<String>>?
    try {
        val element: Json5Element? = compilerOptions.get("paths")
        if (element == null) {
            paths = null
        } else {
            val value: Json5Object = element.asJson5Object
                ?: return Result.err("$parseErrorPrefix: \"paths\" is not an object")

            paths = mutableMapOf()
            val entries = value.entrySet()
            for ((key, element) in entries) {
                if (key == null || element == null) {
                    return Result.err("$parseErrorPrefix: found null in \"paths\"")
                }
                val array: Json5Array = element.asJson5Array
                    ?: return Result.err("$parseErrorPrefix: found non-array value in \"paths\"")

                val pathsValueList = mutableListOf<String>()
                for (element in array) {
                    if (element == null) {
                        return Result.err("$parseErrorPrefix: found null array element in \"paths\"")
                    }
                    val value: String = element.asString
                        ?: return Result.err("$parseErrorPrefix: found non-string array element in \"paths\"")

                    pathsValueList.add(value)
                }

                paths[key] = pathsValueList
            }
        }
    } catch (e: Json5Exception) {
        return Result.err("$parseErrorPrefix: ${e.toString()}")
    }

    val outDir: String?
    try {
        val element: Json5Element? = compilerOptions.get("outDir")
        outDir = if (element == null) {
            null
        } else {
            element.asString
                ?: return Result.err("$parseErrorPrefix: \"outDir\" is not a string")
        }
    } catch (e: Json5Exception) {
        return Result.err("$parseErrorPrefix: ${e.toString()}")
    }

    return Result.ok(TsConfigJson(baseUrl, paths, outDir))
}
