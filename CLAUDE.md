# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ Platform plugin project for TypeScript namespace imports, built using Kotlin and the IntelliJ Platform Gradle Plugin. The project is based on the IntelliJ Platform Plugin Template and targets IntelliJ IDEA Community Edition (IC) platform version 2024.3.6.

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
  - `MyBundle.kt` - Internationalization bundle for plugin messages
  - `services/MyProjectService.kt` - Project-level service implementation
  - `startup/MyProjectActivity.kt` - Plugin startup activity
  - `toolWindow/MyToolWindowFactory.kt` - Tool window factory for UI components
- `src/main/resources/messages/MyBundle.properties` - Localization message properties
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
- **Since Build**: 243 (minimum supported IDE version)
- **Platform**: IntelliJ IDEA Community Edition
- **JVM Target**: Java 21

## Architecture Notes

This is a template-based IntelliJ Platform plugin that provides:
- A tool window extension point for UI components
- A post-startup activity for initialization logic
- Internationalization support through resource bundles
- Project-level services for plugin functionality

The plugin follows IntelliJ Platform plugin development best practices with proper separation of concerns between UI components, services, and configuration.