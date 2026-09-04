---
name: clojure-repl
description: REPL-first Clojure development workflow. Use when using the Clojure REPL, exploring namespaces, finding functions, discovering symbols, evaluating Clojure code, writing Clojure code, testing at the REPL, or following the Explore-Verify-Test-Implement pattern.
---

# Clojure Programming

Every code change follows Explore → Verify → Test → Implement, evaluating through `clj-nrepl-eval` via Bash against the running nREPL.

## Port

Use the port given in your system prompt. Otherwise discover it:

```bash
clj-nrepl-eval --discover-ports
```

One port → use it; several → ask the user (AskUserQuestion); none → ask the user to start nREPL.

## Session

State persists between calls to the same host:port. Re-`require` with `:reload` after file changes; `--reset-session` clears everything.

## Helpers

Load once per session:

```bash
<skill base path>/scripts/load-repl-helpers.sh <PORT>
```

Then, from `clojure-tools-mcp.repl-tools` (referred into your namespace by the loader):

```clojure
(list-ns)  (list-ns "pattern")         ; namespaces
(list-vars 'namespace)                 ; public vars with docs
(doc-namespace 'namespace)
(doc-symbol 'symbol-name)              ; docstring
(source-symbol 'symbol-name)           ; source
(find-symbols "pattern")
(find-specs "pattern")  (describe-spec ::spec-key)
(search-code "pattern")                ; namespaces, symbols, and specs
```

Helpers see only namespaces already `require`d — an empty result means "not loaded", not "doesn't exist".

## CLI

- `-p, --port PORT` (required for eval); `-H, --host HOST` (default 127.0.0.1)
- `-t, --timeout MS` (default 120000)
- `-r, --reset-session`; `-c, --connected-ports`; `-d, --discover-ports`

## The workflow

**Explore** what exists:

```bash
clj-nrepl-eval -p PORT "(require '[target.namespace :as tn] :reload)"
clj-nrepl-eval -p PORT "(list-vars 'target.namespace)"
clj-nrepl-eval -p PORT "(find-symbols \"relevant-keyword\")"
```

**Verify** every function you intend to call — never assume it exists:

```bash
clj-nrepl-eval -p PORT "(doc-symbol 'namespace/function-name)"
clj-nrepl-eval -p PORT "(source-symbol 'namespace/function-name)"
```

**Test** each step on real-shaped data:

```bash
clj-nrepl-eval -p PORT "(def test-data {:example \"real-data-structure\"})"
clj-nrepl-eval -p PORT "(namespace/verified-function test-data)"
```

**Implement** incrementally, validating each piece; heredoc for multi-line forms:

```bash
clj-nrepl-eval -p PORT <<'EOF'
(defn new-function [data]
  (let [step1 (verified-function data)
        step2 (another-function step1)]
    step2))
EOF
clj-nrepl-eval -p PORT "(new-function test-data)"
```

No helper functions or utilities beyond what was requested.
