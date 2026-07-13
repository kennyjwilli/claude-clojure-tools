#!/usr/bin/env bash
set -euo pipefail

# Install Babashka only if missing
command -v bb &>/dev/null || brew install borkdude/brew/babashka

# Install bbin only if missing
command -v bbin &>/dev/null || brew install babashka/brew/bbin

# Ensure ~/.local/bin in PATH
[[ ":$PATH:" == *":$HOME/.local/bin:"* ]] || {
  echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc
  export PATH="$HOME/.local/bin:$PATH"
}

# Install clojure-mcp-light tools (bbin handles updates)
REPO="https://github.com/bhauman/clojure-mcp-light.git"
TAG="v0.2.2"
bbin install "$REPO" --tag "$TAG" --as clj-nrepl-eval --main-opts '["-m" "clojure-mcp-light.nrepl-eval"]'
bbin install "$REPO" --tag "$TAG"
bbin install "$REPO" --tag "$TAG" --as clj-paren-repair --main-opts '["-m" "clojure-mcp-light.paren-repair"]'

echo "Done."
