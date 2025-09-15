import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.FilePath
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TsConfigJson
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TsProjectPath
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

sealed class ModuleEvaluationForTsProject {
    data class BareImport(val moduleName: String, val importPath: String) : ModuleEvaluationForTsProject()
    data class RelativeImport(val moduleName: String) : ModuleEvaluationForTsProject()
    data object ImportDisallowed : ModuleEvaluationForTsProject()
}

fun evaluateModuleForTsProject(
    tsProjectPath: TsProjectPath,
    tsConfigJson: TsConfigJson,
    filePath: FilePath,
): ModuleEvaluationForTsProject {
    val moduleName = makeModuleName(filePath)

    val bareImportPath = makeBareImportPath(tsProjectPath, tsConfigJson, filePath)
    if (bareImportPath != null) {
        return ModuleEvaluationForTsProject.BareImport(moduleName, bareImportPath)
    }

    if (filePath.startsWith(tsProjectPath)) {
        return ModuleEvaluationForTsProject.RelativeImport(moduleName)
    }

    return ModuleEvaluationForTsProject.ImportDisallowed
}

private fun makeModuleName(filePath: FilePath): String {
    return toCamelCase(Paths.get(filePath).nameWithoutExtension)
}

private fun toCamelCase(value: String): String {
    return value.split(" ", "_", "-")
        .mapIndexed { index, word ->
            if (index == 0) word.lowercase()
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
}

private fun makeBareImportPath(
    tsProjectPath: TsProjectPath,
    tsConfigJson: TsConfigJson,
    filePath: FilePath,
): String? {
    val matchedPath = matchPathPatternForProject(tsProjectPath, tsConfigJson, filePath)
    if (matchedPath != null) {
        return Util.pathWithoutExtension(matchedPath)
    }

    if (tsConfigJson.baseUrl != null) {
        val baseUrlPath = Path(tsProjectPath).resolve(tsConfigJson.baseUrl).normalize()
        if (filePath.startsWith(baseUrlPath.toString())) {
            val relativeFilePath = baseUrlPath.relativize(Path(filePath)).toString()

            if (!doesImportPathViaBaseUrlConflictWithPathsMapping(relativeFilePath, tsConfigJson)) {
                return Util.pathWithoutExtension(relativeFilePath)
            }
        }
    }

    return null
}

private fun matchPathPatternForProject(
    tsProjectPath: TsProjectPath,
    tsConfigJson: TsConfigJson,
    filePath: FilePath,
): String? {
    if (tsConfigJson.paths == null) return null

    val baseUrlPath = Path(tsProjectPath)
        .resolve(tsConfigJson.baseUrl ?: ".")
        .normalize()

    for ((pattern, mappings) in tsConfigJson.paths) {
        for (mapping in mappings) {
            val resolvedMapping = baseUrlPath.resolve(mapping).normalize()

            if (pattern.contains('*')) {
                val patternPrefix = pattern.replace("*", "")
                val mappingPrefix = resolvedMapping.toString().replace("*", "")

                if (filePath.startsWith(mappingPrefix)) {
                    val suffix = filePath.substring(mappingPrefix.length)
                    return patternPrefix + suffix
                }
            } else if (filePath == resolvedMapping.toString()) {
                return pattern
            }
        }
    }

    return null
}

fun doesImportPathViaBaseUrlConflictWithPathsMapping(
    relativeFilePath: String,
    tsConfigJson: TsConfigJson,
): Boolean {
    if (tsConfigJson.paths == null) return false

    for (pattern in tsConfigJson.paths.keys) {
        val patternPrefix = if (pattern.contains("*")) {
            pattern.replace("*", "")
        } else {
            pattern
        }
        if (relativeFilePath.startsWith(patternPrefix)) {
            return true
        }
    }
    return false
}
