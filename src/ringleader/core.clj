(ns ringleader.core
  (:use [ringleader.socket :only [create-server]])
  (:gen-class))

(defn echo-server [socket in out]
  (loop []
    (when-let [data (.readLine in)]
      (println data)
      (doto out (.write data) (.write "\n") (.flush))
      (recur))))

(defn -main [& args]
  (println "starting echo server...")
  (create-server echo-server 1234)
  (println "started."))

