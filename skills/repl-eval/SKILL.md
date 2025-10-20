---
name: Clojure REPL Evaluation
description: Evaluate Clojure code in a running nREPL server. Use when testing Clojure expressions, checking function results, debugging code, or when the user mentions REPL, evaluation, or wants to test Clojure code interactively.
allowed-tools: Bash
---

# Clojure REPL Evaluation

Evaluate Clojure expressions in a running nREPL server.

## Usage

```bash
bb repl_eval.clj "(your-code-here)"
```

The script automatically detects the nREPL port from `.nrepl-port` file and returns the result plus any stdout/stderr output.

## Examples

Simple evaluation:
```bash
bb repl_eval.clj "(+ 1 2 3)"
# => 6
```

With side effects:
```bash
bb repl_eval.clj "(do (println \"Debug:\") (+ 10 20))"
# Debug:
# => 30
```

Using namespaces:
```bash
bb repl_eval.clj "(require '[clojure.string :as str]) (str/join \",\" [1 2 3])"
# => "1,2,3"
```

## Notes

- Wrap Clojure code in double quotes and escape shell special characters
- REPL maintains state between evaluations
