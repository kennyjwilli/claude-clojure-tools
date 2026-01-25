---
name: clojure-repl-workflow
description: REPL-first Clojure development workflow. Use when using the Clojure REPL, exploring namespaces, finding functions, discovering symbols, evaluating Clojure code, writing Clojure code, testing at the REPL, or following the Explore-Verify-Test-Implement pattern.
---

# Clojure Programming

## MANDATORY: REPL-First Development Protocol

**CRITICAL REQUIREMENT**: You must ALWAYS follow the "Explore -> Verify -> Test -> Implement" workflow for ALL code changes.

**IMPORTANT**: Use the `clj-nrepl-eval` CLI tool via Bash to execute ALL Clojure code examples shown below. This tool connects to your running nREPL server and evaluates code interactively.

## Autonomous Port Discovery

Before using the REPL, discover the nREPL port:

```bash
clj-nrepl-eval --discover-ports
```

**Workflow:**
1. If **ONE port** found -> use it automatically
2. If **MULTIPLE ports** found -> use AskUserQuestion to let user select
3. If **NO ports** found -> prompt user to start nREPL

## Session Persistence

- Session state persists between calls (same host:port)
- No need to re-require namespaces unless code changed
- Use `:reload` when requiring to pick up file changes

## Loading REPL Helpers

After discovering the port, load the helper functions ONCE per session:

```bash
./scripts/load-repl-helpers.sh <PORT>
```

Example:
```bash
./scripts/load-repl-helpers.sh 61201
```

## The 4-Step Mandatory Workflow

### 1. EXPLORE Existing Code

Use `clj-nrepl-eval` via Bash to load and examine relevant namespaces:

```bash
clj-nrepl-eval -p PORT "(require '[target.namespace :as tn] :reload)"
clj-nrepl-eval -p PORT "(list-vars 'target.namespace)"
clj-nrepl-eval -p PORT "(find-symbols \"relevant-keyword\")"
```

### 2. VERIFY Function Existence & Signatures

**NEVER assume functions exist. Always verify:**

```bash
clj-nrepl-eval -p PORT "(doc-symbol 'namespace/function-name)"
clj-nrepl-eval -p PORT "(source-symbol 'namespace/function-name)"
```

### 3. TEST Logic Incrementally

Create test data and validate each step:

```bash
clj-nrepl-eval -p PORT "(def test-data {:example \"real-data-structure\"})"
clj-nrepl-eval -p PORT "(namespace/verified-function test-data)"
```

### 4. IMPLEMENT Step by Step

Build functions incrementally, testing each piece:

```bash
clj-nrepl-eval -p PORT "(defn new-function [data]
  (let [step1 (verified-function data)
        step2 (another-function step1)]
    step2))"

# Validate immediately
clj-nrepl-eval -p PORT "(new-function test-data)"
```

### Additional Guidelines

- Do not add any extraneous helper functions or utilities beyond what is explicitly requested.

## REPL Helpers Reference

After loading helpers with `./scripts/load-repl-helpers.sh`, these functions are available:

```clojure
;; Namespace exploration
(list-ns)                    ; List all namespaces
(list-ns "pattern")          ; List namespaces matching pattern
(list-vars 'namespace)       ; Show public vars in namespace with docs
(doc-namespace 'namespace)   ; View namespace documentation

;; Symbol exploration
(doc-symbol 'symbol-name)    ; View symbol documentation
(source-symbol 'symbol-name) ; Display source code
(find-symbols "pattern")     ; Find symbols matching pattern

;; Spec exploration
(find-specs "pattern")       ; Find spec keys matching pattern
(describe-spec ::spec-key)   ; Show spec information

;; Combined search
(search-code "pattern")      ; Search namespaces, symbols, and specs
```

**Important**: Require namespaces with `:reload` before using these tools:

```bash
clj-nrepl-eval -p PORT "(require 'my.project.core :reload)"
clj-nrepl-eval -p PORT "(find-symbols \"my-function\")"
```

## Things to Remember

- Use `clj-nrepl-eval -p PORT` for ALL REPL interactions
- Discover port first with `clj-nrepl-eval --discover-ports`
- Load helpers once per session with `./scripts/load-repl-helpers.sh PORT`
- Use search tools extensively (parallel and sequential) to understand codebase
- Session state persists between calls - no need to re-require namespaces unless code changed
- Use `:reload` when requiring to pick up file changes
