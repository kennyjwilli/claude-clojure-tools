# Clojure Tools

A Claude Code plugin for Clojure development that automatically formats your code using Parinfer and enables interactive REPL evaluation.

## Features

### Automatic Parinfer Formatting

Every time Claude Code edits or writes a Clojure file, this plugin automatically formats it using [Parinfer's indent mode](https://github.com/parinfer/parinfer). This ensures your code maintains proper indentation and parenthesis structure without manual intervention.

**Supported file types:**
- `.clj` - Clojure
- `.cljs` - ClojureScript
- `.cljc` - Clojure/ClojureScript
- `.edn` - Extensible Data Notation

### nREPL Evaluation

Evaluate Clojure expressions directly in your nREPL server. Claude Code can test code snippets, verify function behavior, and debug issues by evaluating expressions in your REPL environment.

**Requirements:**
- [Babashka](https://babashka.org/) must be installed
- [Clojure CLI](https://clojure.org/guides/install_clojure) must be installed (required for auto-starting nREPL)

## Installation

### From GitHub

1. Add the marketplace to Claude Code:
```
/plugin marketplace add kennyjwilli/claude-clojure-tools
```

2. Install the plugin:
```
/plugin install clojure-tools@claude-clojure-tools
```

3. Restart Claude Code to activate the plugin

## Configuration

Configuration is **optional** - the plugin works out of the box with sensible defaults. If you need to customize the nREPL behavior, create a `clojure-tools-mcp-config.edn` file in your project root.

### Default Configuration

```clojure
{:nrepl-mode    :always-start
 :nrepl-aliases []
 :nrepl-version "1.5.1"}
```

### Configuration Options

- **`:nrepl-mode`** - Controls how the nREPL server is managed:
  - `:always-start` (default) - Automatically starts a new nREPL server when the MCP starts
  - `:prefer-existing` - Uses an existing nREPL (`.nrepl-port` file) if available, otherwise starts a new one
  - `:require-existing` - Only uses an existing nREPL, fails if not found

- **`:nrepl-aliases`** - Vector of aliases to include when starting the nREPL server (e.g., `[:dev :test]`)

- **`:nrepl-version`** - nREPL version string to use (default: `"1.5.1"`)

## Local Installation (for development)

1. Clone this repository
2. Install dependencies:
```bash
cd claude-clojure-tools
npm install
```

3. Add as a local marketplace:
```
/plugin marketplace add /path/to/claude-clojure-tools
```

4. Install the plugin:
```
/plugin install clojure-tools@claude-clojure-tools
```

## License

MIT

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.
