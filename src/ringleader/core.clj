(ns ringleader.core
  (:use [ringleader.socket :only [create-server create-client]])
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn echo-server [in out]
  (io/copy in out))

(defn- proxy-streams [upstream-in upstream-out downstream-in downstream-out]
  (let [finished (promise)
        downward (future
                   (io/copy upstream-in downstream-out)
                   (deliver finished true))
        upward (future
                 (io/copy downstream-in upstream-out)
                 (deliver finished true))]
    ;; wait for either the upstream *or* downstream to close:
    (deref finished)))
(defn proxy-server [downstream in out]
  (println "connecting to proxy")
  (create-client (partial proxy-streams in out) downstream))


(defn -main [& args]
  ; (create-server echo-server 1234)
  (create-server (partial proxy-server 10001) 10000)
  (println "server started."))

