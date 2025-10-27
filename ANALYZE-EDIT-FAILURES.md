# Edit Failure Analysis Tool

This Babashka script analyzes Claude Code edit operations on Clojure files to identify patterns and statistics about edit failures.

## Usage

```bash
./analyze-edit-failures.clj [projects-dir] [output-file]
```

**Arguments:**
- `projects-dir` (optional): Path to Claude projects directory. Defaults to `~/.claude/projects`
- `output-file` (optional): Path for raw data output. Defaults to `edit-failures.edn`

**Example:**
```bash
# Use defaults
./analyze-edit-failures.clj

# Specify custom paths
./analyze-edit-failures.clj ~/.claude/projects my-failures.edn
```

## What it Does

The script:
1. Scans all `.jsonl` files in your Claude projects directory
2. Extracts Edit and Write operations on Clojure files (`.clj`, `.cljs`, `.cljc`, `.bb`, `.edn`)
3. Matches tool calls with their results to determine success/failure
4. Generates two outputs:
   - **Console output**: Daily statistics and failure breakdown
   - **EDN file**: Complete raw dataset with all context

## Output

### Daily Statistics
Shows per-day metrics including:
- Total edits
- Successful edits (Pass)
- Failed edits (Fail)
- Success percentage (Pass %)

### Failure Type Breakdown
Categorizes failures into:
- `file-modified-since-read`: File changed after Read but before Edit/Write (likely by hook/linter)
- `string-not-found`: Edit old_string doesn't match file content (file changed between edits)
- `file-not-read`: File wasn't read before editing
- `permission-denied`: Edit blocked by user or system
- `user-rejected`: User manually rejected the edit
- `ambiguous-match`: Multiple matches found for old_string
- `no-changes`: old_string and new_string are identical
- `other`: Uncategorized errors


### Raw Data (EDN file)
Each edit record includes:
```clojure
{:tool-name "Edit"              ; or "Write"
 :file-path "/path/to/file.clj"
 :timestamp "2025-10-26T12:00:00Z"
 :session-id "uuid"
 :success true                  ; or false
 :error-message nil             ; or error string
 :old-string "..."              ; for Edit operations
 :new-string "..."              ; what was written
 :tool-use-id "toolu_xxx"
 :result-timestamp "..."}
```

## Example Output

```
=== Daily Edit Statistics (Clojure files only) ===

|      :date | :total | :pass | :fail |  :pass% |
|------------+--------+-------+-------+---------|
| 2025-10-27 |     32 |    25 |     7 |  78.13% |
| 2025-10-26 |    148 |   114 |    34 |  77.03% |
| 2025-10-25 |     32 |    29 |     3 |  90.63% |

=== Daily Failure Breakdown ===

|      :date | :ambiguous-match | :file-modified-since-read | :file-not-read | :no-changes | :permission-denied | :string-not-found | :user-rejected |
|------------+------------------+---------------------------+----------------+-------------+--------------------+-------------------+----------------|
| 2025-10-27 |                0 |                         0 |              3 |           0 |                  0 |                 4 |              0 |
| 2025-10-26 |                1 |                        23 |              1 |           0 |                  0 |                 9 |              0 |
| 2025-10-25 |                0 |                         1 |              0 |           0 |                  0 |                 2 |              0 |

Overall: 373 total edits, 67 failures (17.96%)

=== Failure Type Breakdown ===

file-modified-since-read    :  27 (40.3% of failures)
string-not-found            :  20 (29.9% of failures)
file-not-read               :  10 (14.9% of failures)
permission-denied           :   5 (7.5% of failures)
user-rejected               :   2 (3.0% of failures)
ambiguous-match             :   2 (3.0% of failures)
no-changes                  :   1 (1.5% of failures)
```
