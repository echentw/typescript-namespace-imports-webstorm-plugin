# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## In-progress plan

See plan.md

## Project Overview

This is a WebStorm/IntelliJ Platform plugin for TypeScript namespace imports, built using Kotlin and the IntelliJ Platform Gradle Plugin. The plugin provides intelligent autocomplete for namespace imports (e.g., `import * as moduleName from 'path/to/module'`) by scanning TypeScript files and respecting tsconfig.json configuration.

**Key Features:**
- Scans project for .ts/.tsx files and generates camelCase module names for autocomplete
- Respects tsconfig.json settings including baseUrl, paths, and outDir
- Real-time file system watching for incremental updates
- Supports both bare imports (via baseUrl/paths) and relative imports
- Automatically inserts import statements at the top of files
- Configurable quote style for import statements (single or double quotes)

## Development Commands

### Building and Testing
- `./gradlew build` - Assembles and tests the project
- `./gradlew test` - Runs the test suite
- `./gradlew check` - Runs all verification tasks including tests and code coverage
- `./gradlew clean` - Deletes the build directory

### Plugin Development
- `./gradlew buildPlugin` - Builds the plugin and prepares ZIP archive for deployment
- `./gradlew runIde` - Runs IDE instance with the plugin loaded for testing
- `./gradlew runIdeForUiTests` - Runs IDE instance configured for UI testing
- `./gradlew verifyPlugin` - Runs Plugin Verifier to check binary compatibility
- `./gradlew publishPlugin` - Publishes plugin to JetBrains Marketplace (requires tokens)

### Code Quality and Coverage
- `./gradlew koverHtmlReport` - Generates HTML coverage report
- `./gradlew koverXmlReport` - Generates XML coverage report for CI/CD
- `./gradlew koverVerify` - Performs coverage verification

## Project Structure

### Core Plugin Files
- `src/main/resources/META-INF/plugin.xml` - Plugin configuration and extension points
- `gradle.properties` - Plugin metadata, platform versions, and build configuration
- `build.gradle.kts` - Gradle build configuration with IntelliJ Platform plugin setup

### Source Code Organization
- `src/main/kotlin/com/github/echentw/typescriptnamespaceimportswebstormplugin/` - Main plugin source
  - `completion/ExtensionCompletionContributor.kt` - Provides TypeScript namespace import completions
  - `services/ExtensionService.kt` - Main service interface and project-level service implementation
  - `services/NamespaceImportService.kt` - Core business logic for module discovery and completion
  - `settings/` - Configuration classes for plugin settings (quote style, etc.)
  - `startup/ExtensionProjectActivity.kt` - Plugin startup activity that initializes services
  - `util/` - Utility functions for tsconfig.json parsing and module evaluation
- `src/test/kotlin/` - Test source code following same package structure

### Configuration Files
- `gradle/libs.versions.toml` - Gradle version catalog for dependency management
- `CHANGELOG.md` - Plugin change notes extracted for plugin marketplace
- `codecov.yml` - Code coverage reporting configuration
- `qodana.yml` - Qodana code quality analysis configuration

## Plugin Configuration

The plugin is configured for:
- **Plugin ID**: `com.github.echentw.typescriptnamespaceimports`
- **Plugin Name**: "Typescript Namespace Imports"  
- **Since Build**: 242 (minimum supported IDE version)
- **Platform**: WebStorm (WS) 2024.2.4
- **JVM Target**: Java 21
- **Dependencies**: Gson for JSON parsing, JSON5 for tsconfig.json parsing with comments

## Architecture Overview

The plugin operates through three main components:

1. **Completion System** (`ExtensionCompletionContributor`)
   - Triggers on any character input in TypeScript files
   - Provides namespace import suggestions based on user query
   - Automatically inserts import statements at file top when completions are selected

2. **Service Layer** (`ExtensionService` + `NamespaceImportService`)
   - `ExtensionServiceImpl`: Project-level service that manages initialization and file watching
   - `NamespaceImportServiceImpl`: Core business logic for module discovery and completion matching
   - Uses concurrent data structures for thread-safe caching of TypeScript projects and modules

3. **File System Integration**
   - Real-time file system monitoring via `BulkFileListener`
   - Incremental updates for .ts/.tsx file changes (create, delete, move, rename)
   - Full rescan on tsconfig.json changes
   - Efficient caching with modules grouped by query first character for fast lookups

## Development Notes

### IntelliJ Completion API Behavior
- The `addCompletions()` method in a `CompletionContributor` does NOT get called for every character the user types. Instead, it follows a two-phase process:
  1. **Generation Phase**: `addCompletions()` is called when the user starts typing (typically on the first character)
  2. **Filtering Phase**: As the user continues typing, IntelliJ filters the existing completion results without calling `addCompletions()` again
  - Practical Implications:
    - ❌ `if (prefix.contains("asdf"))` - Won't work because by the time user types "asdf", the method isn't being called
    - ✅ `if ("asdf".startsWith(prefix))` - Works because it catches partial prefixes like "a", "as", "asd" during the initial generation phase

### Data Flow
1. **Startup**: `ExtensionProjectActivity` initializes the `ExtensionService`
2. **Initialization**: Service scans for tsconfig.json files, then indexes all .ts/.tsx files
3. **Module Evaluation**: Each file is evaluated against each TypeScript project to determine import type (bare/relative/disallowed)
4. **Caching**: Modules are cached by first character of module name for efficient lookup
5. **Completion**: User typing triggers completion contributor which queries cached modules by prefix

### Performance Optimizations
- **Character-based indexing**: Modules cached by first character to avoid scanning all possibilities
- **Concurrent data structures**: Thread-safe maps allow background file system updates
- **Incremental updates**: Individual file changes update cache without full rescan
- **Project-aware caching**: Each TypeScript project maintains its own module cache

### Debugging
- Use `println()` statements that appear in the `./gradlew runIde` terminal output
- Check `Util.playground()` method for debugging utilities
