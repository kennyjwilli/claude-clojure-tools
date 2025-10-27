#!/usr/bin/env bb

(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[cheshire.core :as json]
         '[clojure.pprint :as pprint])

(def clojure-extensions #{".clj" ".cljs" ".cljc" ".bb" ".edn"})

(defn clojure-file? [file-path]
  (when file-path
    (some #(str/ends-with? file-path %) clojure-extensions)))

(defn parse-timestamp [ts]
  (when ts
    (try
      (-> ts
          (str/split #"T")
          first)
      (catch Exception _ nil))))

(defn extract-tool-calls
  "Extract Edit/Write tool calls from assistant messages"
  [entry]
  (when (and (= "assistant" (:type entry))
             (get-in entry [:message :content]))
    (let [content (get-in entry [:message :content])]
      (when (sequential? content)
        (->> content
             (filter #(and (map? %)
                           (contains? #{"Edit" "Write"} (:name %))
                           (clojure-file? (get-in % [:input :file_path]))))
             (map (fn [tool]
                    {:tool-use-id (:id tool)
                     :tool-name (:name tool)
                     :file-path (get-in tool [:input :file_path])
                     :old-string (get-in tool [:input :old_string])
                     :new-string (get-in tool [:input :new_string])
                     :timestamp (:timestamp entry)
                     :session-id (:sessionId entry)})))))))

(defn extract-tool-result
  "Extract tool result from user messages"
  [entry tool-calls-map]
  (when (and (= "user" (:type entry))
             (:toolUseResult entry))
    (let [result (:toolUseResult entry)
          ;; Try to find the tool-use-id from the message content
          tool-use-id (some-> entry :message :content first :tool_use_id)]
      (when-let [tool-call (get tool-calls-map tool-use-id)]
        (let [failed? (and (string? result)
                           (or (str/includes? result "Error:")
                               (str/includes? result "String to replace not found")
                               (str/includes? result "not found in file")))
              success? (and (map? result)
                            (:filePath result))]
          (when (or failed? success?)
            (assoc tool-call
                   :success (not failed?)
                   :error-message (when failed? (first (str/split-lines result)))
                   :result-timestamp (:timestamp entry))))))))

(defn process-jsonl-file
  "Process a single JSONL file and extract edit operations"
  [file-path]
  (let [lines (line-seq (io/reader file-path))
        entries (keep #(try (json/parse-string % true)
                            (catch Exception _ nil))
                      lines)
        ;; First pass: collect all tool calls
        tool-calls (->> entries
                        (mapcat extract-tool-calls)
                        (remove nil?))
        ;; Create lookup map by tool-use-id
        tool-calls-map (into {} (map (juxt :tool-use-id identity) tool-calls))
        ;; Second pass: match results to tool calls
        results (->> entries
                     (keep #(extract-tool-result % tool-calls-map))
                     (remove nil?))]
    results))

(defn process-all-projects
  "Process all JSONL files in Claude projects directory"
  [projects-dir]
  (let [project-dirs (file-seq (io/file projects-dir))
        jsonl-files (->> project-dirs
                         (filter #(.isFile %))
                         (filter #(str/ends-with? (.getName %) ".jsonl")))]
    (println (format "Processing %d JSONL files..." (count jsonl-files)))
    (->> jsonl-files
         (mapcat #(do
                    (print ".")
                    (flush)
                    (process-jsonl-file (.getPath %))))
         (doall))))

(defn categorize-failure
  "Categorize the type of failure based on error message"
  [error-msg]
  (cond
    (str/includes? error-msg "String to replace not found") :string-not-found
    (str/includes? error-msg "not found in file") :string-not-found
    (str/includes? error-msg "modified since read") :file-modified-since-read
    (str/includes? error-msg "File has not been read") :file-not-read
    (str/includes? error-msg "Permission to edit") :permission-denied
    (str/includes? error-msg "user doesn't want to proceed") :user-rejected
    (str/includes? error-msg "matches of the string") :ambiguous-match
    (str/includes? error-msg "old_string and new_string are exactly the same") :no-changes
    :else :other))

(defn calculate-daily-stats
  "Calculate daily statistics from edit results"
  [results]
  (->> results
       (group-by #(parse-timestamp (:timestamp %)))
       (map (fn [[date edits]]
              (let [total (count edits)
                    failures (count (filter #(not (:success %)) edits))
                    successes (- total failures)
                    failure-rate (if (pos? total)
                                   (* 100.0 (/ failures total))
                                   0.0)
                    ;; Calculate failure type breakdown
                    failure-types (->> edits
                                       (filter #(not (:success %)))
                                       (group-by #(categorize-failure (or (:error-message %) "")))
                                       (map (fn [[type fails]] [type (count fails)]))
                                       (into {})
                                       (sort-by second >))]
                {:date date
                 :total total
                 :successes successes
                 :failures failures
                 :success-rate (- 100.0 failure-rate)
                 :failure-types failure-types})))
       (sort-by :date)
       reverse))

(defn print-daily-stats
  "Print daily statistics as a formatted table"
  [stats]
  (println "\n\n=== Daily Edit Statistics (Clojure files only) ===\n")

  ;; Print main stats table
  (let [table-data (map (fn [{:keys [date total successes failures success-rate]}]
                          {:date (or date "unknown")
                           :total total
                           :pass successes
                           :fail failures
                           :pass% (format "%.2f%%" success-rate)})
                        stats)]
    (pprint/print-table [:date :total :pass :fail :pass%] table-data))

  (println)

  ;; Print daily failure breakdown table with failure types as columns
  (println "=== Daily Failure Breakdown ===\n")
  (let [;; Collect all unique failure types across all days
        all-failure-types (->> stats
                               (mapcat :failure-types)
                               (map first)
                               (distinct)
                               (sort))
        ;; Build rows with date and counts for each failure type
        breakdown-data (map (fn [{:keys [date failure-types]}]
                              (let [failure-map (into {} failure-types)
                                    row (assoc failure-map :date date)]
                                ;; Fill in 0 for missing failure types
                                (reduce (fn [r type]
                                          (if (contains? r type)
                                            r
                                            (assoc r type 0)))
                                        row
                                        all-failure-types)))
                            stats)
        ;; Only show rows that have at least one failure
        breakdown-data (filter (fn [row]
                                 (some #(pos? (get row % 0)) all-failure-types))
                               breakdown-data)
        ;; Define column order: date first, then failure types
        columns (vec (cons :date all-failure-types))]
    (when (seq breakdown-data)
      (pprint/print-table columns breakdown-data)))

  (println)
  (let [total-edits (reduce + (map :total stats))
        total-failures (reduce + (map :failures stats))
        overall-failure-rate (if (pos? total-edits)
                               (* 100.0 (/ total-failures total-edits))
                               0.0)]
    (println (format "Overall: %d total edits, %d failures (%.2f%%)"
                     total-edits
                     total-failures
                     overall-failure-rate))))

(defn save-raw-data
  "Save raw edit data to EDN file"
  [results output-file]
  (println (format "\nSaving raw data to %s..." output-file))
  (with-open [w (io/writer output-file)]
    (binding [*out* w]
      (pprint/pprint
       {:generated-at (str (java.time.Instant/now))
        :edits results})))
  (println (format "Saved %d edit records" (count results))))

(defn print-failure-breakdown
  "Print breakdown of failure types"
  [results]
  (let [failures (filter #(not (:success %)) results)
        total-failures (count failures)
        total-edits (count results)
        by-type (->> failures
                     (group-by #(categorize-failure (or (:error-message %) "")))
                     (map (fn [[type fails]]
                            [type (count fails)]))
                     (into {}))]

    (println "\n=== Failure Type Breakdown ===\n")

    ;; Sort by count descending
    (doseq [[type cnt] (sort-by second > by-type)]
      (println (format "%-28s: %3d (%.1f%% of failures)"
                       (name type)
                       cnt
                       (* 100.0 (/ cnt (max 1 total-failures))))))

    (println (format "\nTotal failures: %d" total-failures))))


(defn -main [& args]
  (let [projects-dir (or (first args)
                         (str (System/getProperty "user.home") "/.claude/projects"))
        output-file (or (second args) "edit-failures.edn")]

    (println (format "Analyzing Claude Code edit operations in: %s" projects-dir))
    (println "Filtering for Clojure files: .clj, .cljs, .cljc, .bb, .edn\n")

    (let [results (process-all-projects projects-dir)
          stats (calculate-daily-stats results)]

      ;; Print statistics
      (print-daily-stats stats)

      ;; Print failure breakdown
      (print-failure-breakdown results)

      ;; Save raw data
      (save-raw-data results output-file)

      (println "\nDone!"))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
