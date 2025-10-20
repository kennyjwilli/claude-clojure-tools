#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import parinfer from 'parinfer';

// Read hook event data from stdin
let inputData = '';

process.stdin.on('data', (chunk) => {
  inputData += chunk;
});

process.stdin.on('end', () => {
  try {
    const event = JSON.parse(inputData);

    // Extract file path from the tool result
    // PostToolUse hook provides tool name and result
    const toolName = event.tool_name;
    const toolInput = event.tool_input;

    // Only process Edit and Write tools
    if (toolName !== 'Edit' && toolName !== 'Write') {
      process.exit(0);
    }

    // Get file path from tool input
    const filePath = toolInput?.file_path;

    if (!filePath) {
      console.error('[clojure-tools] No file path found in tool input');
      process.exit(0);
    }

    // Check if file is a Clojure file
    const ext = path.extname(filePath);
    const clojureExtensions = ['.clj', '.cljs', '.cljc', '.edn'];

    if (!clojureExtensions.includes(ext)) {
      process.exit(0);
    }

    // Check if file exists
    if (!fs.existsSync(filePath)) {
      console.error(`[clojure-tools] File not found: ${filePath}`);
      process.exit(0);
    }

    // Read file content
    const content = fs.readFileSync(filePath, 'utf8');

    // Apply parinfer indentMode
    const result = parinfer.indentMode(content);

    if (result.success) {
      // Write formatted content back to file
      fs.writeFileSync(filePath, result.text, 'utf8');
      console.log(`[clojure-tools] Formatted ${path.basename(filePath)}`);
    } else {
      // Log error but don't interrupt workflow
      console.error(`[clojure-tools] Parinfer formatting failed for ${filePath}:`, result.error);
    }

  } catch (error) {
    // Fail silently - just log the error
    console.error('[clojure-tools] Error in format-clojure hook:', error.message);
  }

  process.exit(0);
});
