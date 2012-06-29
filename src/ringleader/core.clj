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
(defn proxy-server [in out]

  (letfn [(proxy-to-client [client c-in c-out]
            (proxy-streams in out c-in c-out))]
    (println "connecting to proxy")
    (create-client proxy-to-client 10001)))

(defn -main [& args]
  ; (create-server echo-server 1234)
  (create-server proxy-server 10000)
  (println "server started."))

