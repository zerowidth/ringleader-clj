(ns ringleader.core
  (:use [ringleader.socket :only [create-server create-client proxy-streams]]
        [ringleader.app :only [start-app]])
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn echo-server [in out]
  (io/copy in out))

(defn proxy-server [downstream in out]
  (println "connecting to proxy")
  (create-client (partial proxy-streams in out) downstream))

(defn get-running-app [{:keys [starting started cmd]}]
  (let [me (.getId (Thread/currentThread))]
    (println me "getting app")
    (if (= me (swap! starting (fn [id] (or id me))))
      (do
        (println "starting app...")
        (let [app-started? (start-app cmd)]
          (println me "app started?" app-started?)
          (deliver started app-started?)))
      (do
        (println me "lost the race, waiting for start...")
        (deref started)))))

(defn app-connection [app in out]
  (let [me (.getId (Thread/currentThread))]
    (println me "app connection")
    (when (nil? (deref (:starting app)))
      (println me "app not running")
      (get-running-app app))
    (println me "checking if app running")
    (if (deref (:started app))
      (do
        (println me "proxying!")
        (create-client (partial proxy-streams in out) (:downstream app)))
      (println "could not start app!"))))

(defn create-app-server [port downstream-port cmd]
  (let [app {:cmd cmd
             :downstream downstream-port
             :starting (atom nil)
             :started (promise)}]
    (create-server (partial app-connection app) port)))

(defn -main [& args]
  ; (create-server echo-server 10000)
  ; (create-server (partial proxy-server 10001) 10000)
  (create-app-server 10000 10001 "echo 'app sleeping' && sleep 5 && echo 'app listening' && ncat -k -l 10001")
  (println "server started."))

