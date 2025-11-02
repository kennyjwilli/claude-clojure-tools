#!/bin/bash

# Smoke tests for clojure-tools-mcp
# This script runs clojure.test-based smoke tests from the example-project directory context

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Running MCP smoke tests with clojure.test..."
echo "Project root: $PROJECT_ROOT"
echo ""

# Change to example-project directory to provide a Clojure project context
cd "$PROJECT_ROOT/example-project"

# Run the smoke tests using Babashka
bb "$SCRIPT_DIR/smoke_test.clj"
