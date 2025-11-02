#!/usr/bin/env bb

(ns smoke-test
  (:require
    [babashka.process :as bp]
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing use-fixtures run-tests]]))

;; Dynamic vars to hold MCP server connection
(def ^:dynamic *writer* nil)
(def ^:dynamic *reader* nil)
(def ^:dynamic *process* nil)

(def message-id (atom 0))

(defn next-id []
  (swap! message-id inc))

(defn send-request!
  "Send a JSON-RPC request to the MCP server."
  [writer method params]
  (let [id (next-id)
        request {:jsonrpc "2.0"
                 :id id
                 :method method
                 :params params}]
    (.write writer (str (json/generate-string request) "\n"))
    (.flush writer)
    id))

(defn try-parse-json
  "Try to parse a line as JSON. Returns parsed JSON or nil if it fails."
  [line]
  (try
    (json/parse-string line true)
    (catch Exception _
      nil)))

(defn read-response!
  "Read a JSON-RPC response from the MCP server.
  Skips non-JSON lines (like nREPL startup messages)."
  [reader]
  (loop []
    (when-let [line (.readLine reader)]
      (let [trimmed (str/trim line)]
        (if (str/starts-with? trimmed "{")
          (if-let [parsed (try-parse-json line)]
            parsed
            (recur))
          ;; Not a JSON line, skip it
          (recur))))))

(defn start-mcp-server!
  "Start the MCP server and return process, writer, and reader."
  []
  (let [script-path "../mcp/clojure_tools_mcp.clj"
        process (bp/process ["bb" script-path]
                           {:err :inherit
                            :shutdown bp/destroy-tree})
        writer (io/writer (:in process))
        reader (io/reader (:out process))]
    ;; Give the server more time to start nREPL and initialize
    (Thread/sleep 3000)
    {:process process :writer writer :reader reader}))

(defn stop-mcp-server!
  "Stop the MCP server and clean up resources."
  [{:keys [writer reader process]}]
  (try
    (.close writer)
    (.close reader)
    (bp/destroy-tree process)
    (catch Exception _
      ;; Ignore cleanup errors
      nil)))

(defn with-mcp-server
  "Test fixture that starts and stops the MCP server."
  [f]
  (let [{:keys [process writer reader]} (start-mcp-server!)]
    (binding [*process* process
              *writer* writer
              *reader* reader]
      (try
        (f)
        (finally
          (stop-mcp-server! {:process process :writer writer :reader reader}))))))

(use-fixtures :once with-mcp-server)

;;
;; Tests
;;

(deftest test-initialize
  (testing "Server initialization"
    ;; Add a small delay to ensure server is fully started
    (Thread/sleep 500)
    (let [id (send-request! *writer* "initialize" {:protocolVersion "2024-11-05"
                                                    :capabilities {}
                                                    :clientInfo {:name "smoke-test" :version "1.0.0"}})
          response (read-response! *reader*)]
      (is (some? response) "Initialize returns response")
      (is (= id (:id response)) "Initialize has correct ID")
      (is (some? (:result response)) "Initialize has result")
      (is (some? (get-in response [:result :protocolVersion])) "Initialize has protocolVersion"))))

(deftest test-tools-list
  (testing "Tools list method"
    (let [id (send-request! *writer* "tools/list" {})
          response (read-response! *reader*)
          tools (get-in response [:result :tools])]
      (is (some? response) "Tools list returns response")
      (is (= id (:id response)) "Tools list has correct ID")
      (is (seq tools) "Tools list has tools")
      (is (some #(= "repl_eval" (:name %)) tools) "Tools list contains repl_eval"))))

(deftest test-basic-eval
  (testing "Basic expression evaluation"
    (let [id (send-request! *writer* "tools/call" {:name "repl_eval"
                                                    :arguments {:code "(+ 1 2)"}})
          response (read-response! *reader*)
          content (-> response :result :content first :text)
          parsed (json/parse-string content true)
          values (:values parsed)]
      (is (some? response) "Basic eval returns response")
      (is (= id (:id response)) "Basic eval has correct ID")
      (is (nil? (:error response)) "Basic eval has no error")
      (is (seq values) "Basic eval has values")
      (is (= "3" (first values)) "Basic eval result is 3"))))

(deftest test-stdout-capture
  (testing "Stdout capture"
    (let [id (send-request! *writer* "tools/call" {:name "repl_eval"
                                                    :arguments {:code "(println \"test output\") 42"}})
          response (read-response! *reader*)
          content (-> response :result :content first :text)
          parsed (json/parse-string content true)
          stdout (:stdout parsed)
          values (:values parsed)]
      (is (some? response) "Stdout capture returns response")
      (is (nil? (:error response)) "Stdout capture has no error")
      (is (and stdout (str/includes? stdout "test output")) "Stdout contains 'test output'")
      (is (seq values) "Stdout eval has return value")
      ;; println returns nil, so the last value should be 42
      (is (= "42" (last values)) "Stdout eval result is 42"))))

(deftest test-exception-handling
  (testing "Exception handling"
    (let [id (send-request! *writer* "tools/call" {:name "repl_eval"
                                                    :arguments {:code "(throw (ex-info \"test error\" {:type :test}))"}})
          response (read-response! *reader*)
          content (-> response :result :content first :text)
          parsed (json/parse-string content true)
          stderr (:stderr parsed)]
      (is (some? response) "Exception handling returns response")
      (is (nil? (:error response)) "Exception handling has no top-level error")
      (is (some? stderr) "Exception captured in stderr")
      (is (and stderr (str/includes? stderr "test error")) "Stderr contains 'test error'")
      (is (and stderr (str/includes? stderr "clojure.lang")) "Stderr contains stacktrace"))))

(deftest test-timeout-handling
  (testing "Timeout handling"
    (let [id (send-request! *writer* "tools/call" {:name "repl_eval"
                                                    :arguments {:code "(Thread/sleep 60000)"
                                                                :timeout 2}})
          start-time (System/currentTimeMillis)
          response (read-response! *reader*)
          elapsed (- (System/currentTimeMillis) start-time)
          content (some-> response :result :content first :text)]
      (is (some? response) "Timeout returns response")
      (is (and content (or (str/includes? (str/lower-case content) "timeout")
                          (str/includes? (str/lower-case content) "timed out")))
          (str "Timeout response contains error message. Content: " content))
      (is (< elapsed 5000) (str "Timeout occurs within reasonable time. Elapsed: " elapsed "ms")))))

(defn -main
  "Run the smoke tests."
  [& args]
  (let [{:keys [fail error]} (run-tests 'smoke-test)]
    (System/exit (if (zero? (+ fail error)) 0 1))))

;; If running as a script, execute tests
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
