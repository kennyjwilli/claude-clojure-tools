(ns example-test
  (:require [clojure.test :refer :all]))

(defn oops
  [x]
  (inc x))

(deftest foo-test
  (is (= 2 (+ 1 1))))

(deftest bar-test
  (Thread/sleep 1000)
  (is (= true true)))
