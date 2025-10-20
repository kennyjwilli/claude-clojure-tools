#!/usr/bin/env bb

(ns repl-eval
  (:require [bencode.core :as bencode]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn read-nrepl-port
  "Read the nREPL port from .nrepl-port file in current directory."
  []
  (let [port-file (io/file ".nrepl-port")]
    (when-not (.exists port-file)
      (binding [*out* *err*]
        (println "Error: .nrepl-port file not found in current directory.")
        (println "Make sure your nREPL server is running and has created a .nrepl-port file."))
      (System/exit 1))
    (try
      (Integer/parseInt (str/trim (slurp port-file)))
      (catch Exception e
        (binding [*out* *err*]
          (println "Error: Could not parse .nrepl-port file:" (.getMessage e)))
        (System/exit 1)))))

(defn nrepl-eval
  "Evaluate a Clojure expression via nREPL and return the result.
  Returns a map with :value, :out, and :err keys."
  [port code]
  (try
    (let [socket (java.net.Socket. "localhost" port)
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
              ;; bencode returns status as vector of byte arrays
              done? (and status-bytes (some #{"done"} (map #(String. ^bytes %) status-bytes)))

              ;; Accumulate results
              result' (cond-> result
                        value-bytes
                        (assoc :value (String. ^bytes value-bytes))

                        out-bytes
                        (update :out str (String. ^bytes out-bytes))

                        err-bytes
                        (update :err str (String. ^bytes err-bytes)))]

          (if done?
            (do
              (.close socket)
              result')
            (recur result')))))
    (catch java.net.ConnectException e
      (binding [*out* *err*]
        (println "Error: Could not connect to nREPL server on port" port)
        (println "Make sure your nREPL server is running."))
      (System/exit 1))
    (catch Exception e
      (binding [*out* *err*]
        (println "Error during nREPL evaluation:")
        (println (.getMessage e)))
      (System/exit 1))))

(defn -main [& args]
  (when (empty? args)
    (binding [*out* *err*]
      (println "Usage: bb repl_eval.clj \"(your clojure code)\"")
      (println "Example: bb repl_eval.clj \"(+ 1 2 3)\""))
    (System/exit 1))

  (let [code (first args)
        port (read-nrepl-port)
        result (nrepl-eval port code)]

    ;; Print stdout if present
    (when-not (str/blank? (:out result))
      (print (:out result))
      (flush))

    ;; Print stderr if present
    (when-not (str/blank? (:err result))
      (binding [*out* *err*]
        (print (:err result))
        (flush)))

    ;; Print the value
    (when (:value result)
      (println (:value result)))))

(apply -main *command-line-args*)
