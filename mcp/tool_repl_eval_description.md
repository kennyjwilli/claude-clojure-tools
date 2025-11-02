Evaluate Clojure code in a running nREPL server. Use this tool to execute Clojure expressions, test functions, inspect values, debug code, or perform any Clojure computation.

## Parameters

- `code` (required): The Clojure code to evaluate
- `timeout` (optional): Timeout in seconds (default: 30). If evaluation exceeds this time, it will be interrupted and an error will be returned.

## How it works

The tool connects to a nREPL server and evaluates the provided Clojure code, returning:
- The evaluation results (values for all expressions)
- Any stdout output (from println, print, etc.)
- Any stderr output (errors, warnings)

The REPL maintains state between evaluations - namespace definitions, vars, and loaded libraries persist across calls.

## Code Exploration Helpers

The `clojure-tools-mcp.repl-tools` namespace provides functions for discovering and understanding code:

**Search functions** (accept string or regex patterns):
- `(search-code "pattern")` - Search across namespaces, symbols, and specs
- `(find-namespaces "pattern")` - Find namespace names matching pattern
- `(find-symbols "pattern")` - Find qualified symbols matching pattern
- `(find-specs "pattern")` - Find spec keys matching pattern

**Documentation functions**:
- `(list-ns)` - List all namespaces
- `(list-vars 'namespace)` - Show public vars in namespace with docs
- `(doc-namespace 'namespace)` / `(doc-symbol 'symbol)` - View documentation
- `(source-symbol 'symbol)` - Display source code
- `(describe-spec 'spec)` - Show spec information

Example workflow:
```clojure
(require '[clojure-tools-mcp.repl-tools :as repl])
(repl/search-code "auth")            ; Find authentication-related code
(repl/doc-namespace 'myapp.auth)     ; Understand the namespace
(repl/list-vars 'myapp.auth)         ; See all functions
(repl/source-symbol 'myapp.auth/login) ; View source
```

## Error handling

If evaluation fails (syntax errors, runtime exceptions, etc.), the error message will be returned in the tool response.
