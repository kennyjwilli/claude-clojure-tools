#!/usr/bin/env bb

(ns clojure-tools-mcp
  (:require [bencode.core :as bencode]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :as json]))

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
  []
  (let [port-file (io/file ".nrepl-port")]
    (when-not (.exists port-file)
      (throw (ex-info "No .nrepl-port file found" {:type :no-nrepl-port})))
    (Integer/parseInt (str/trim (slurp port-file)))))

(defn nrepl-eval
  "Evaluate a Clojure expression via nREPL and return the result."
  [{:keys [port code]}]
  (with-open [socket (java.net.Socket. "localhost" port)
              out-stream (.getOutputStream socket)
              in-stream (java.io.PushbackInputStream. (.getInputStream socket))]

    ;; Send eval request
    (bencode/write-bencode out-stream {"op" "eval" "code" code})

    ;; Read responses until we get "done" status
    (loop [result {:value nil :out "" :err ""}]
      (let [response (bencode/read-bencode in-stream)
            value-bytes (get response "value")
            out-bytes (get response "out")
            err-bytes (get response "err")
            status-bytes (get response "status")
            done? (and status-bytes (some #{"done"} (map #(String. ^bytes %) status-bytes)))
            result' (cond-> result
                      value-bytes
                      (assoc :value (String. ^bytes value-bytes))
                      out-bytes
                      (update :out str (String. ^bytes out-bytes))
                      err-bytes
                      (update :err str (String. ^bytes err-bytes)))]
        (if done?
          result'
          (recur result'))))))

(defn handle-initialize [_]
  {:protocolVersion "2024-11-05"
   :capabilities    {:tools {}}
   :serverInfo      {:name    "clojure-tools-mcp"
                     :version "1.0.0"}})

(defn handle-tools-list [_]
  {:tools
   [{:name        "repl_eval"
     :description (read-script-file "tool_repl_eval_description.md")
     :inputSchema {:type       "object"
                   :properties {:code {:type        "string"
                                       :description "The Clojure code to evaluate"}}
                   :required   ["code"]}}]})

(defn handle-tools-call [params]
  (let [tool-name (get params "name")
        arguments (get params "arguments")]
    (case tool-name
      "repl_eval"
      (try
        (let [code (get arguments "code")
              port (read-nrepl-port)
              result (nrepl-eval {:port port :code code})
              output (str/join "\n" (filter seq [(:out result) (:err result) (:value result)]))]
          {:content [{:type "text"
                      :text output}]})
        (catch Exception e
          {:content [{:type "text"
                      :text (str "Error: " (.getMessage e))}]
           :isError true}))

      {:content [{:type "text"
                  :text (str "Unknown tool: " tool-name)}]
       :isError true})))

(defn handle-request [request]
  (let [method (get request "method")
        params (get request "params")
        id (get request "id")]
    (try
      (let [result (case method
                     "initialize" (handle-initialize params)
                     "tools/list" (handle-tools-list params)
                     "tools/call" (handle-tools-call params)
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
  []
  (let [port (read-nrepl-port)
        repl-helpers-code (read-script-file "repl_helpers.clj")]
    (nrepl-eval {:port port :code repl-helpers-code})
    true))

(defn -main [& args]
  (repl-eval-init!)
  ;; Read JSON-RPC messages from stdin and respond on stdout
  (let [reader (io/reader *in*)]
    (loop []
      (when-let [line (.readLine reader)]
        (when-not (str/blank? line)
          (try
            (let [request (json/parse-string line)
                  response (handle-request request)]
              (println (json/generate-string response))
              (flush))
            (catch Exception e
              (binding [*out* *err*]
                (println "Error processing request:" (.getMessage e))))))
        (recur)))))

(apply -main *command-line-args*)
