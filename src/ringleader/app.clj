(ns ringleader.app
  (:import [java.io IOException])
  (:use [ringleader.util :only [on-thread]]
        [ringleader.socket :only [wait-for-port]])
  (:require [conch.core :as sh]))

(defn start-app [cmd]
  (try
    (let [proc (sh/proc "bash" "-c" cmd)
          port-opened (future (wait-for-port 10001))
          exit-code (future (sh/exit-code proc))
          started (promise)]
      (on-thread #(sh/stream-to proc :out (System/out)))
      (on-thread #(sh/stream-to proc :err (System/err)))
      (on-thread (fn []
                   (Thread/sleep 1000)
                   (println "checking")
                   (cond
                     (realized? port-opened) (do
                                               (println "port openend")
                                               (deliver started @port-opened))
                     (realized? exit-code) (do
                                             (println "proc exited")
                                             (future-cancel port-opened)
                                             (deliver started false))
                     :else (recur))))
      (deref started))
  (catch IOException e
    (println "could not run command:" (.getMessage e)))))

