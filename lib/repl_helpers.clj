(ns clojure-tools-mcp.repl-tools
  "Namespace containing helper functions for REPL-driven development"
  (:require [clojure.pprint :as pprint]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

;; Namespace exploration
(defn list-ns
  "List all available namespaces, sorted alphabetically."
  []
  (let [namespaces (sort (map str (all-ns)))]
    (println "Available Namespaces:")
    (doseq [ns-name namespaces]
      (println (str "  " ns-name)))
    (println (str "\nTotal: " (count namespaces) " namespaces"))
    nil))

(defn list-vars
  "List all public vars in the given namespace with their arglists and docstrings.
   ns-name can be a symbol or string."
  [ns-nm]
  (let [ns-obj (if (symbol? ns-nm)
                 (find-ns ns-nm)
                 (find-ns (symbol ns-nm)))]
    (if ns-obj
      (let [vars (sort-by first (ns-publics ns-obj))]
        (println (str "Vars in " (ns-name ns-obj) ":"))
        (println (str "-------------------------------------------"))
        (doseq [[sym var] vars]
          (let [m (meta var)]
            (println (str (name sym)))
            (when-let [arglists (:arglists m)]
              (println (str "  " arglists)))
            (when-let [doc (:doc m)]
              (println (str "  " doc)))
            (println)))
        (println (str "Total: " (count vars) " vars")))
      (println (str "Error: Namespace not found: " ns-nm)))
    nil))

;; Symbol exploration
(defn doc-symbol
  "Show documentation for a symbol. Accepts symbol or string."
  [sym]
  (if-let [v (resolve (if (symbol? sym) sym (symbol sym)))]
    (let [m (meta v)]
      (println (str "-------------------------"))
      (println (str (:name m) " - " (or (:doc m) "No documentation")))
      (println (str "  Defined in: " (:ns m)))
      (when-let [arglists (:arglists m)]
        (println (str "  Arguments: " arglists)))
      (when-let [added (:added m)]
        (println (str "  Added in: " added)))
      (when-let [fn-spec (s/get-spec (symbol v))]
        (println "  Function Spec: " (with-out-str (pprint/pprint (s/describe fn-spec)))))
      (when-let [deprecated (:deprecated m)]
        (println (str "  DEPRECATED: " deprecated)))
      (println (str "-------------------------")))
    (println (str "Error: Symbol not found: " sym)))
  nil)

(defn doc-namespace
  "Show documentation for a namespace. Accepts symbol or string."
  [sym]
  (if-let [ns (find-ns (if (symbol? sym) sym (symbol sym)))]
    (let [m (meta ns)]
      (println (str "-------------------------"))
      (println (str (name sym) " - " (or (:doc m) "No documentation")))
      (println (str "-------------------------")))
    (println (str "Error: Namespace not found: " sym)))
  nil)

(defn source-symbol
  "Show source code for a var. Accepts symbol or string."
  [sym]
  (if-let [v (resolve (if (symbol? sym) sym (symbol sym)))]
    (if-let [source-fn (requiring-resolve 'clojure.repl/source-fn)]
      (println (source-fn (symbol v)))
      (println "Error: clojure.repl/source-fn not available"))
    (println (str "Error: Symbol not found: " sym)))
  nil)

(defn describe-spec
  "Show detailed information about a keyword/symbol/var `spec`."
  [spec]
  (if-let [spec-form (s/get-spec spec)]
    (do
      (println (str "-------------------------"))
      (println (str "Spec: " spec))
      (pprint/pprint (s/describe spec-form))
      (println (str "-------------------------")))
    (println (str "Error: Spec not found: " spec)))
  nil)

(defn- pattern->match-fn
  [pattern]
  (if (instance? java.util.regex.Pattern pattern)
    #(re-find pattern %)
    #(str/includes? % (str pattern))))

(defn find-namespaces
  "Find namespaces matching the given pattern.
   Pattern can be a string (for substring matching) or a regex pattern.
   Returns a vector of matching namespace names."
  [pattern]
  (let [match-fn (pattern->match-fn pattern)]
    (vec (sort (for [ns (all-ns)
                     :let [ns-name (str (ns-name ns))]
                     :when (match-fn ns-name)]
                 ns-name)))))

(defn find-symbols
  "Find symbols matching the given pattern across all namespaces.
   Pattern can be a string (for substring matching) or a regex pattern.
   Matches against both namespace and symbol name in the format 'namespace/symbol'.
   Returns a vector of matching qualified symbol names."
  [pattern]
  (let [match-fn (pattern->match-fn pattern)]
    (vec (sort (for [ns (all-ns)
                     :let [ns-name (str (ns-name ns))]
                     [sym-name] (ns-publics ns)
                     :let [qualified-name (str ns-name "/" (name sym-name))]
                     :when (match-fn qualified-name)]
                 qualified-name)))))

(defn find-specs
  "Find specs matching the given pattern in the spec registry.
   Pattern can be a string (for substring matching) or a regex pattern.
   Searches both keyword specs (from s/def) and symbol specs (from s/fdef).
   Returns a vector of matching spec keys (keywords or symbols)."
  [pattern]
  (let [match-fn (pattern->match-fn pattern)]
    (vec (sort-by str (for [spec-key (keys (s/registry))
                            :let [spec-name (str spec-key)]
                            :when (match-fn spec-name)]
                        spec-key)))))

(defn search-code
  "Search for namespaces, symbols, and specs matching the given pattern.
   Pattern can be a string (for substring matching) or a regex pattern.
   Prints results in separate sections for namespaces, symbols, and specs."
  [pattern]
  (let [namespaces (find-namespaces pattern)
        symbols (find-symbols pattern)
        specs (find-specs pattern)]

    ;; Print namespaces
    (println (str "Namespaces matching '" pattern "':"))
    (if (empty? namespaces)
      (println "  (none)")
      (doseq [ns namespaces]
        (println (str "  " ns))))
    (println (str "Total: " (count namespaces) " namespaces\n"))

    ;; Print symbols
    (println (str "Symbols matching '" pattern "':"))
    (if (empty? symbols)
      (println "  (none)")
      (doseq [sym symbols]
        (println (str "  " sym))))
    (println (str "Total: " (count symbols) " symbols\n"))

    ;; Print specs
    (println (str "Specs matching '" pattern "':"))
    (if (empty? specs)
      (println "  (none)")
      (doseq [spec specs]
        (println (str "  " spec))))
    (println (str "Total: " (count specs) " specs"))

    nil))
