# Clojure Tools

A Claude Code plugin for Clojure development that automatically formats your code using Parinfer and provides a REPL-first development workflow.

## Features

### REPL-First Development Workflow

The plugin includes a skill that guides Claude through a structured REPL-first workflow:
1. **Explore** - Load and examine existing code
2. **Verify** - Check function signatures and documentation
3. **Test** - Validate logic incrementally
4. **Implement** - Build functions step by step

Uses `clj-nrepl-eval` CLI to evaluate Clojure expressions in your running nREPL server.

## Installation

### Prerequisites

Install the required CLI tools:

```bash
./scripts/install-clojure-repl-tools.sh
```

This installs:
- [Babashka](https://babashka.org/) - Fast Clojure scripting
- [bbin](https://github.com/babashka/bbin) - Babashka binary installer
- `clj-nrepl-eval` - nREPL evaluation CLI
- `clj-paren-repair` - Parenthesis repair tool (used by hooks)

### Plugin Installation

1. Add the marketplace to Claude Code:
```
/plugin marketplace add kennyjwilli/claude-clojure-tools
```

2. Install the plugin:
```
/plugin install clojure-tools@claude-clojure-tools
```

3. Restart Claude Code to activate the plugin

## Usage

### Starting a REPL Session

1. Start an nREPL server in your project (e.g., `clj -M:dev` or via your editor)

2. Claude will discover the port automatically:
```bash
clj-nrepl-eval --discover-ports
```

3. Load the REPL helpers for code exploration:
```bash
./scripts/load-repl-helpers.sh <PORT>
```

### REPL Helpers

After loading helpers, these functions are available:

```clojure
;; Namespace exploration
(list-ns)                    ; List all namespaces
(list-vars 'namespace)       ; Show public vars with docs
(doc-namespace 'namespace)   ; View namespace documentation

;; Symbol exploration
(doc-symbol 'sym)            ; View symbol documentation
(source-symbol 'sym)         ; Display source code
(find-symbols "pattern")     ; Find symbols matching pattern

;; Spec exploration
(find-specs "pattern")       ; Find spec keys matching pattern
(describe-spec ::key)        ; Show spec information

;; Combined search
(search-code "pattern")      ; Search namespaces, symbols, and specs
```

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
