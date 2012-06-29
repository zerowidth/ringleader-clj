(ns ringleader.core
  (:use [ringleader.socket :only [create-server create-client proxy-streams]])
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn echo-server [in out]
  (io/copy in out))

(defn proxy-server [downstream in out]
  (println "connecting to proxy")
  (create-client (partial proxy-streams in out) downstream))


(defn -main [& args]
  ; (create-server echo-server 1234)
  (create-server (partial proxy-server 10001) 10000)
  (println "server started."))

