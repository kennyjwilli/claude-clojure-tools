Evaluate Clojure code in a running nREPL server. Use this tool to execute Clojure expressions, test functions, inspect values, debug code, or perform any Clojure computation.

## When to use this tool

- Testing Clojure expressions and seeing their results
- Calling functions to check their output
- Inspecting variable values or namespace contents
- Debugging code by evaluating expressions in context
- Performing calculations or data transformations
- Loading and requiring namespaces

## Code Exploration Helpers

The `clojure-tools-mcp.repl-tools` namespace provides helper functions for exploring codebases. Use these extensively to discover and understand code:

- `(list-ns)` - List all available namespaces
- `(list-vars 'namespace)` - Show all public vars in a namespace with docs and arglists
- `(doc-symbol 'symbol)` - View detailed documentation for a symbol
- `(source-symbol 'symbol)` - Display source code for a var
- `(find-symbols "pattern")` - Search for symbols matching a pattern across all namespaces

Examples:
```clojure
;; Discover what namespaces exist
(require '[clojure-tools-mcp.repl-tools :as repl])
(repl/list-ns)

;; Explore a namespace's API
(repl/list-vars 'clojure.string)

;; Find all functions with "map" in the name
(repl/find-symbols "map")
```

## Prerequisites

IMPORTANT: This tool requires a running nREPL server with a `.nrepl-port` file in the current working directory. The tool will fail if no nREPL server is running.

## How it works

The tool connects to the nREPL server via the port specified in `.nrepl-port`, evaluates the provided Clojure code, and returns:
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
