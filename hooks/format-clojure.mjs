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
    const clojureExtensions = ['.clj', '.cljs', '.cljc', '.cljd', '.bb', '.edn'];

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
      // Check if formatting changed the content
      const contentChanged = result.text !== content;

      // Write formatted content back to file
      fs.writeFileSync(filePath, result.text, 'utf8');

      if (contentChanged) {
        // Output JSON to inform Claude about the formatting change
        // The thinking here is that Claude knowing a change occurred could decrease chance of edit failures.
        const output = {
          hookSpecificOutput: {
            hookEventName: 'PostToolUse',
            additionalContext: `[clojure-tools] Auto-formatted ${path.basename(filePath)} with parinfer. You may want to re-read it to see the updated formatting.`
          }
        };
        console.log(JSON.stringify(output));
      } else {
        // No changes, just log normally
        console.log(`[clojure-tools] No formatting changes needed for ${path.basename(filePath)}`);
      }
    } else {
      // Parinfer failed - report error to Claude so it can fix the issue
      const error = result.error || {};
      const errorType = error.name || 'unknown';
      const errorLine = error.lineNo !== undefined ? error.lineNo + 1 : 'unknown'; // parinfer uses 0-indexed lines
      const errorCol = error.x !== undefined ? error.x + 1 : 'unknown'; // parinfer uses 0-indexed columns
      const errorMsg = error.message || 'formatting failed';

      const output = {
        decision: 'block',
        reason: `[clojure-tools] Parinfer formatting error in ${path.basename(filePath)}: ${errorType} at line ${errorLine}, column ${errorCol} - ${errorMsg}. Please fix the syntax issue and try again.`
      };
      console.log(JSON.stringify(output));
    }

  } catch (error) {
    // Fail silently - just log the error
    console.error('[clojure-tools] Error in format-clojure hook:', error.message);
  }

  process.exit(0);
});
