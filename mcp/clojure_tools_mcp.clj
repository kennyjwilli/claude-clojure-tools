#!/usr/bin/env bb

(ns clojure-tools-mcp
  (:require
    [bencode.core :as bencode]
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]))

;; MCP Protocol Implementation (JSON-RPC over stdio)

(defn read-script-file
  "Read a file from the same directory as this script."
  [filename]
  (try
    (let [script-dir (-> *file* io/file .getParent)
          file (io/file script-dir filename)]
      (when (.exists file)
        (slurp file)))
    (catch Exception e
      nil)))

(defn read-nrepl-port
  "Read the nREPL port from .nrepl-port file in current directory."
  [& {:keys [path] :or {path ".nrepl-port"}}]
  (let [port-file (io/file path)]
    (when-not (.exists port-file)
      (throw (ex-info "No .nrepl-port file found" {:type :no-nrepl-port})))
    (Integer/parseInt (str/trim (slurp port-file)))))

(def current-id (atom 0))

(defn next-id
  "Generate a unique message ID for nREPL requests."
  []
  (str (swap! current-id inc)))

(defn bytes->str
  "Convert bytes to string, or return original value if not bytes."
  [x]
  (if (bytes? x)
    (String. ^bytes x)
    (str x)))

(defn read-msg
  "Read and parse a bencode message, converting byte arrays to strings."
  [response]
  (let [res (zipmap (map keyword (keys response))
                    (map #(if (bytes? %)
                            (String. ^bytes %)
                            %)
                         (vals response)))
        res (if-let [status (:status res)]
              (assoc res :status (mapv bytes->str status))
              res)
        res (if-let [sessions (:sessions res)]
              (assoc res :sessions (mapv bytes->str sessions))
              res)]
    res))

(defn read-reply
  "Read nREPL responses until we get one matching the session and id."
  [read-fn session id]
  (loop []
    (let [msg (read-msg (read-fn))]
      (if (and (= (:session msg) session)
               (= (:id msg) id))
        msg
        (recur)))))

(defn with-nrepl-connection
  "Execute function f with nREPL connection. Provides write and read functions."
  [{:keys [host port] :or {host "localhost"}} handler]
  (with-open [socket (java.net.Socket. ^String host ^long port)
              out-stream (.getOutputStream socket)
              in-stream (java.io.PushbackInputStream. (.getInputStream socket))]
    (handler {:write (fn [msg] (bencode/write-bencode out-stream msg))
              :read  (fn [] (bencode/read-bencode in-stream))})))

(defn nrepl-clone-session
  "Create a new nREPL session and return the session ID."
  [{:keys [port]}]
  (with-nrepl-connection {:port port}
    (fn [{:keys [write read]}]
      ;; Send clone request with unique ID
      (let [id (next-id)]
        (write {"op" "clone" "id" id})
        ;; Read response matching our ID
        (let [response (read-msg (read))]
          (when-not (:new-session response)
            (throw (ex-info "Failed to clone nREPL session" {:response response})))
          (:new-session response))))))

(defn nrepl-eval
  "Evaluate a Clojure expression via nREPL and return the result."
  [{:keys [port code session-id timeout-seconds]}]
  (with-nrepl-connection {:port port}
    (fn [{:keys [write read]}]
      (let [id (next-id)
            _ (write {"op" "eval" "code" code "session" session-id "id" id})
            read-loop (future
                        (loop [result {:responses []}]
                          (let [response (read-reply read session-id id)
                                done? (some #(= % "done") (:status response))
                                result' (update result :responses conj response)]
                            (if done?
                              result'
                              (recur result')))))]
        (let [result (deref read-loop (* timeout-seconds 1000) ::timeout)]
          (if (= result ::timeout)
            (do
              (write {"op" "interrupt" "session" session-id})
              (throw (ex-info (str "Evaluation timed out after " timeout-seconds " seconds")
                       {:type :timeout :timeout-seconds timeout-seconds})))
            result))))))

(defn process-nrepl-result
  "Process nREPL evaluation result into a structured map.
  Extracts :stdout, :stderr, :values, and :ex from the responses.
  Only includes keys with non-empty values."
  [result]
  (let [responses (:responses result)
        stdout-parts (keep :out responses)
        stderr-parts (keep :err responses)
        value-parts (keep :value responses)
        ex-parts (keep :ex responses)]
    (cond-> {}
      (seq value-parts)
      (assoc :values (vec value-parts))
      (seq stdout-parts)
      (assoc :stdout (str/join stdout-parts))
      (seq stderr-parts)
      (assoc :stderr (str/join stderr-parts))
      (seq ex-parts)
      (assoc :ex (str/join " " ex-parts)))))

(comment
  (def port (read-nrepl-port :path "example-project/.nrepl-port"))
  (def port 5567)
  (def session-id (nrepl-clone-session {:port port}))
  (nrepl-eval {:port            port
               :code            "(do (require '[example-test] :reload) (in-ns 'example-test))"
               :session-id      session-id
               :timeout-seconds 30})
  (nrepl-eval {:port            port
               :code            "(require '[example-test] :reload) "
               :session-id      session-id
               :timeout-seconds 30})

  (def r (nrepl-eval {:port            port
                :code            "(example-test/oops :a)"
                :session-id      session-id
                :timeout-seconds 30}))

  (nrepl-eval {:port            port
               :code            "(require '[other-file] :reload) "
               :session-id      session-id
               :timeout-seconds 30})
  (nrepl-eval {:port            port
               :code            "(clojure.test/run-tests)"
               :session-id      session-id
               :timeout-seconds 30})
  (nrepl-eval {:port            port
               :code            "(clojure.stacktrace/print-stack-trace *e)"
               :session-id      session-id
               :timeout-seconds 30})

  (def r (nrepl-eval {:port            port
                :code            "(str 1)"
                :session-id      session-id
                :timeout-seconds 30}))

  (process-nrepl-result r)
  )


(defn handle-initialize [_]
  {:protocolVersion "2024-11-05"
   :capabilities    {:tools {}}
   :serverInfo      {:name    "clojure-tools-mcp"
                     :version "1.0.0"}})

(defn handle-tools-list [_]
  {:tools
   [{:name         "repl_eval"
     :description  (read-script-file "tool_repl_eval_description.md")
     :inputSchema  {:type       "object"
                    :properties {:code    {:type        "string"
                                           :description "The Clojure code to evaluate"}
                                 :timeout {:type        "number"
                                           :description "Timeout in seconds (default: 30)"
                                           :default     30}}
                    :required   ["code"]}
     :outputSchema {:type       "object"
                    :properties {:values {:type        "array"
                                          :items       {:type "string"}
                                          :description "Array of return values from the evaluated expression(s)"}
                                 :stdout {:type        "string"
                                          :description "Standard output from the evaluation"}
                                 :stderr {:type        "string"
                                          :description "Standard error from the evaluation"}
                                 :ex     {:type        "string"
                                          :description "Exception information if an error occurred"}}}}]})

(defn handle-tools-call [{:keys [session-id]} params]
  (let [tool-name (get params "name")
        arguments (get params "arguments")]
    (case tool-name
      "repl_eval"
      (try
        (let [code (get arguments "code")
              timeout (get arguments "timeout" 30)
              port (read-nrepl-port)
              result (nrepl-eval {:port port :code code :session-id session-id :timeout-seconds timeout})
              structured (process-nrepl-result result)]
          {:content           [{:type "text"
                                :text (json/generate-string structured)}]
           :structuredContent structured})
        (catch Exception e
          {:content [{:type "text"
                      :text (str "Error: " (ex-message e))}]
           :isError true}))

      {:content [{:type "text"
                  :text (str "Unknown tool: " tool-name)}]
       :isError true})))

(defn handle-request [{:keys [request session-id]}]
  (let [method (get request "method")
        params (get request "params")
        id (get request "id")]
    (try
      (let [result (case method
                     "initialize" (handle-initialize params)
                     "tools/list" (handle-tools-list params)
                     "tools/call" (handle-tools-call {:session-id session-id} params)
                     {:error {:code    -32601
                              :message (str "Method not found: " method)}})]
        (if (:error result)
          {:jsonrpc "2.0"
           :id      id
           :error   (:error result)}
          {:jsonrpc "2.0"
           :id      id
           :result  result}))
      (catch Exception e
        {:jsonrpc "2.0"
         :id      id
         :error   {:code    -32603
                   :message (str "Internal error: " (.getMessage e))}}))))

(defn repl-eval-init!
  [{:keys [port session-id]}]
  (let [repl-helpers-code (read-script-file "repl_helpers.clj")]
    (nrepl-eval {:port port :code repl-helpers-code :session-id session-id :timeout-seconds 10})
    true))

(defn -main [& args]
  (let [port (read-nrepl-port)
        session-id (nrepl-clone-session {:port port})]
    (repl-eval-init! {:port port :session-id session-id})
    ;; Read JSON-RPC messages from stdin and respond on stdout
    (let [reader (io/reader *in*)]
      (loop []
        (when-let [line (.readLine reader)]
          (when-not (str/blank? line)
            (try
              (let [request (json/parse-string line)
                    response (handle-request {:request request :session-id session-id})]
                (println (json/generate-string response))
                (flush))
              (catch Exception e
                (binding [*out* *err*]
                  (println "Error processing request:" (.getMessage e))))))
          (recur))))))

;(apply -main *command-line-args*)
