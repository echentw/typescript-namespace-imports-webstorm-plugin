import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TsConfigJson
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TsProject
import com.github.echentw.typescriptnamespaceimportswebstormplugin.services.TsProjectPath
import com.intellij.openapi.vfs.VirtualFile
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

sealed class ModuleEvaluationForTsProject {
    data class BareImport(val moduleName: String, val importPath: String) : ModuleEvaluationForTsProject()
    data class RelativeImport(val moduleName: String) : ModuleEvaluationForTsProject()
    class ImportDisallowed : ModuleEvaluationForTsProject()
}

fun evaluateModuleForTsProject(
    tsProjectPath: TsProjectPath,
    tsProject: TsProject,
    file: VirtualFile,
): ModuleEvaluationForTsProject {
    val moduleName = makeModuleName(file)

    val bareImportPath = makeBareImportPath(tsProjectPath, tsProject, file)
    if (bareImportPath != null) {
        return ModuleEvaluationForTsProject.BareImport(moduleName, bareImportPath)
    }

    if (file.path.startsWith(tsProjectPath)) {
        return ModuleEvaluationForTsProject.RelativeImport(moduleName)
    }

    return ModuleEvaluationForTsProject.ImportDisallowed()
}

private fun makeModuleName(file: VirtualFile): String {
    return toCamelCase(file.nameWithoutExtension)
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
    tsProject: TsProject,
    file: VirtualFile,
): String? {
    val matchedPath = matchPathPatternForProject(tsProjectPath, tsProject, file)
    if (matchedPath != null) {
        return Util.pathWithoutExtension(matchedPath)
    }

    // TODO: check this logic
    if (tsProject.tsConfigJson.baseUrl != null) {
        val baseUrlPath = Path(tsProjectPath).resolve(tsProject.tsConfigJson.baseUrl)
        if (file.path.startsWith(baseUrlPath.toString())) {
            val relativeFilePath = baseUrlPath.relativize(Path(file.path)).toString()

            if (!doesImportPathViaBaseUrlConflictWithPathsMapping(relativeFilePath, tsProject.tsConfigJson)) {
                return Util.pathWithoutExtension(relativeFilePath)
            }
        }
    }

    return null
}

private fun matchPathPatternForProject(
    tsProjectPath: TsProjectPath,
    tsProject: TsProject,
    file: VirtualFile,
): String? {
    if (tsProject.tsConfigJson.paths == null) return null

    val baseUrl = tsProject.tsConfigJson.baseUrl ?: "."
    val baseUrlPath = Path(tsProjectPath).resolve(baseUrl)

    for ((pattern, mappings) in tsProject.tsConfigJson.paths) {
        for (mapping in mappings) {
            val resolvedMapping = baseUrlPath.resolve(mapping)

            if (pattern.contains('*')) {
                val patternPrefix = pattern.replace("*", "")
                val mappingPrefix = resolvedMapping.toString().replace("*", "")

                if (file.path.startsWith(mappingPrefix)) {
                    val suffix = file.path.substring(mappingPrefix.length)
                    return patternPrefix + suffix
                }
            } else if (file.path == resolvedMapping.toString()) {
                return pattern
            }
        }
    }

    return null
}

fun doesImportPathViaBaseUrlConflictWithPathsMapping(
    importPath: String,
    tsConfigJson: TsConfigJson,
): Boolean {
    if (tsConfigJson.paths == null) return false

    for (pattern in tsConfigJson.paths.keys) {
        val patternPrefix = if (pattern.contains("*")) {
            pattern.replace("*", "")
        } else {
            pattern
        }
        if (importPath.startsWith(pattern)) {
            return true
        }
    }
    return false
}