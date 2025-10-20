# Clojure Tools

A Claude Code plugin for Clojure development that automatically formats your code using Parinfer.

## Features

### Automatic Parinfer Formatting

Every time Claude Code edits or writes a Clojure file, this plugin automatically formats it using [Parinfer's indent mode](https://github.com/parinfer/parinfer). This ensures your code maintains proper indentation and parenthesis structure without manual intervention.

**Supported file types:**
- `.clj` - Clojure
- `.cljs` - ClojureScript
- `.cljc` - Clojure/ClojureScript
- `.edn` - Extensible Data Notation

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

### Local Installation (for development)

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

## How It Works

This plugin uses a PostToolUse hook that:
1. Monitors Edit and Write tool operations
2. Detects when a Clojure file has been modified
3. Automatically applies Parinfer indent mode formatting
4. Saves the formatted result back to the file


## Roadmap

- [ ] nREPL evaluation support for interactive development
- [ ] Integration with clj-kondo for linting

## License

MIT

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.
