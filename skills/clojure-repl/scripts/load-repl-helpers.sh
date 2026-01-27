#!/usr/bin/env bash
# Usage: ./scripts/load-repl-helpers.sh <PORT>
# Loads REPL helper functions into the specified nREPL session

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <PORT>"
  echo "Example: $0 61201"
  exit 1
fi

PORT="$1"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HELPERS_PATH="$SCRIPT_DIR/../lib/repl_helpers.clj"

if [[ ! -f "$HELPERS_PATH" ]]; then
  echo "Error: repl_helpers.clj not found at $HELPERS_PATH"
  exit 1
fi

clj-nrepl-eval -p "$PORT" "(do (load-file \"$HELPERS_PATH\") (require '[clojure-tools-mcp.repl-tools :refer :all]) (#'clojure-tools-mcp.repl-tools/print-loaded-help))"
