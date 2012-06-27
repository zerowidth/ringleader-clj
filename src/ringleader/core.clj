(ns ringleader.core
  (:use [ringleader.socket :only [create-server]])
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn echo-server [socket in out]
  (io/copy in out))

(defn -main [& args]
  (println "starting echo server...")
  (create-server echo-server 1234)
  (println "started."))

