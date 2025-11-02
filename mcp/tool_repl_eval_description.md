Evaluate Clojure code in a running nREPL server. Use this tool to execute Clojure expressions, test functions, inspect values, debug code, or perform any Clojure computation.

## Parameters

- `code` (required): The Clojure code to evaluate
- `timeout` (optional): Timeout in seconds (default: 30). If evaluation exceeds this time, it will be interrupted and an error will be returned.

## When to use this tool

- Testing Clojure expressions and seeing their results
- Calling functions to check their output
- Inspecting variable values or namespace contents
- Debugging code by evaluating expressions in context
- Performing calculations or data transformations
- Loading and requiring namespaces

## Code Exploration Helpers

**IMPORTANT: Always explore unfamiliar codebases before writing code.** The `clojure-tools-mcp.repl-tools` namespace provides essential helper functions for discovery and understanding.

**Typical workflow:** Start with `search-code` to find relevant namespaces, symbols, and specs → Use `doc-namespace` to understand a namespace's purpose → Use `list-vars` to explore the namespace's API → Use `doc-symbol`, `source-symbol`, or `describe-spec` for detailed information about specific code artifacts.

Available helpers:

**Search functions** (accept both string for substring matching and regex patterns):
- `(search-code "pattern")` - **PRIMARY SEARCH TOOL** - Comprehensively search across namespaces, symbols, and specs, printing results in organized sections
- `(find-namespaces "pattern")` - Returns vector of namespace names matching the pattern
- `(find-symbols "pattern")` - Returns vector of qualified symbol names matching the pattern
- `(find-specs "pattern")` - Returns vector of spec keys (keywords from `s/def`, symbols from `s/fdef`) matching the pattern

**Exploration and documentation functions**:
- `(list-ns)` - List all available namespaces
- `(list-vars 'namespace)` - Show all public vars in a namespace with docs and arglists
- `(doc-namespace 'namespace)` - View namespace-level documentation
- `(doc-symbol 'symbol)` - View detailed documentation for a symbol
- `(source-symbol 'symbol)` - Display source code for a var
- `(describe-spec 'spec)` - Show detailed spec information

Examples:
```clojure
(require '[clojure-tools-mcp.repl-tools :as repl])

;; PRIMARY: Search across all code artifacts (namespaces, symbols, and specs)
(repl/search-code "user")
;; Prints organized sections showing matching namespaces, symbols, and specs

;; Search with regex patterns for more precise matching
(repl/search-code #"map$")
;; Finds items ending with "map"

;; Complete workflow example: search → understand → explore → details
(repl/search-code "auth")           ; Find authentication-related code
(repl/doc-namespace 'myapp.auth)    ; Understand the auth namespace
(repl/list-vars 'myapp.auth)        ; See all functions in the namespace
(repl/doc-symbol 'myapp.auth/login) ; Get detailed docs for login function
(repl/describe-spec :myapp.auth/credentials) ; View spec for credentials

;; Browse all available namespaces
(repl/list-ns)
```

## How it works

The tool connects to the running nREPL server and evaluates the provided Clojure code, and returns:
- The evaluation result (the value of the last expression)
- Any stdout output (from println, print, etc.)
- Any stderr output (errors, warnings)

## State and side effects

- The REPL maintains state between evaluations
- Namespace definitions, vars, and loaded libraries persist across calls
- Side effects (file writes, database operations, etc.) will actually execute
- Each evaluation happens in the same REPL session

## Examples

Simple arithmetic:
```clojure
(+ 1 2 3)
;; Returns: "6"
```

String manipulation:
```clojure
(clojure.string/upper-case "hello")
;; Returns: "HELLO"
```

With side effects:
```clojure
(do (println "Debug output") (+ 10 20))
;; Stdout: "Debug output"
;; Returns: "30"
```

Requiring namespaces:
```clojure
(require '[clojure.string :as str])
(str/join ", " [1 2 3])
;; Returns: "1, 2, 3"
```

Defining and using functions:
```clojure
(defn square [x] (* x x))
(square 5)
;; Returns: "25"
```

Data structure operations:
```clojure
(map inc [1 2 3 4 5])
;; Returns: "(2 3 4 5 6)"
```

## Error handling

If evaluation fails (syntax errors, runtime exceptions, etc.), the error message will be returned in the tool response. Examine the error output to understand what went wrong.
