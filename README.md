# Typescript namespace imports (Webstorm plugin)

![Build](https://github.com/echentw/typescript-namespace-imports-webstorm-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

This plugin helps you with namespace imports when writing code.

A [namespace import](http://exploringjs.com/es6/ch_modules.html#_importing-styles) is an import like:
```
import * as moduleName from 'path/to/module_name';
```

## Features

This plugin offers the camelCase version of every typescript file in your workspace as a module inside of autocomplete.

For example if the file `module_name` exists in your
workspace, it will offer to import it as a module called
`moduleName`.

- As you type "moduleNa", you will see "moduleName" as an autocomplete suggestion.
- If you select it, then `import * as moduleName from 'path/to/module_name';` will automatically be added to the top of the file.

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "typescript-namespace-imports-webstorm-plugin"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/echentw/typescript-namespace-imports-webstorm-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
