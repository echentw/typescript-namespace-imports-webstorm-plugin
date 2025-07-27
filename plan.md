# WebStorm TypeScript Namespace Import Plugin - Development Plan

## Project Goal
Create a WebStorm plugin that helps with autocompleting namespace imports in TypeScript. When the user types "mapU", the plugin should suggest `import * as mapUtil from 'common/map_util';` as an autocomplete option.

## Requirements
- Handle multiple TypeScript projects with their own tsconfig.json files
- Respect `compilerOptions.paths`, `baseUrl`, and `outDir` settings
- Ignore node_modules/ and output directories
- Support intelligent prefix matching for namespace imports

## Development Phases

### Phase 1: Basic Plugin Foundation ⏳
**Step 1: Hello World Completion** ✅
- [x] Create a basic `CompletionContributor` that triggers when user types "asdf"
- [x] Simply adds "!" at the end → "asdf!"
- [x] Learn the IntelliJ completion API basics

**Step 2: Expand Basic Completion** ❌ (Won't do)
- [x] ~~Make it work with any 4-letter prefix (not just "asdf")~~ - Skipping to focus on core functionality
- [x] ~~Add multiple completion options~~ - Skipping to focus on core functionality  
- [x] ~~Learn about completion contexts and filtering~~ - Skipping to focus on core functionality

### Phase 2: TypeScript File Discovery
**Step 3: File System Scanning** ✅
- [x] Scan project for all `.ts` and `.tsx` files
- [x] Store file paths in a simple list/map
- [x] Ignore basic patterns like `node_modules/` for now

**Step 4: Extract Module Names** ✅
- [x] Parse file paths to generate potential import names
- [x] Example: `src/common/map_util.ts` → suggest "mapUtil" prefix
- [x] Store mapping of prefix → file path

### Phase 3: Basic Import Suggestions
**Step 5: Simple Import Completion** ✅
- [x] When user types "mapU", suggest `import * as mapUtil from './path/to/file'`
- [x] Use relative paths for now (no tsconfig.json parsing yet)
- [x] Test with a few hardcoded examples

### Phase 4: TypeScript Project Integration
**Step 6: Find tsconfig.json Files**
- [ ] Locate all `tsconfig.json` files in the project
- [ ] Parse basic properties: `baseUrl`, `paths`, `outDir`
- [ ] Handle multiple TypeScript projects in one workspace

**Step 7: Path Resolution**
- [ ] Use `baseUrl` and `paths` mapping to generate correct import paths
- [ ] Convert file system paths to TypeScript module paths
- [ ] Example: `src/common/map_util.ts` + baseUrl → `common/map_util`

### Phase 5: Smart Filtering & Performance
**Step 8: Advanced Filtering**
- [ ] Exclude `node_modules/`, `outDir`, and other irrelevant paths
- [ ] Respect `.gitignore` patterns
- [ ] Add configuration options for custom exclusions

**Step 9: Performance & Caching**
- [ ] Cache file scans and tsconfig parsing
- [ ] Incremental updates when files change
- [ ] Debounce file system watchers

### Phase 6: Polish & Edge Cases
**Step 10: Enhanced Matching**
- [ ] Improve prefix matching (camelCase, snake_case, kebab-case)
- [ ] Handle nested directories intelligently
- [ ] Add fuzzy matching capabilities

**Step 11: UI/UX Improvements**
- [ ] Better completion item presentation
- [ ] Icons and styling
- [ ] Configuration panel in IDE settings

## Current Status
✅ **Completed**: Step 5 - Simple Import Completion  
🎯 **Ready for**: Step 6 - Find tsconfig.json Files

## Notes
- Starting with simple "asdf!" completion to learn IntelliJ Platform plugin development
- Each step builds incrementally on the previous one
- Focus on getting basic functionality working before adding complex TypeScript parsing logic

## Architecture Decisions
- TBD as we progress through implementation

## Testing Strategy
- TBD as we progress through implementation