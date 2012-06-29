(ns ringleader.core
  (:use [ringleader.socket :only [create-server create-client proxy-streams]])
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn echo-server [in out]
  (io/copy in out))

(defn proxy-server [downstream in out]
  (println "connecting to proxy")
  (create-client (partial proxy-streams in out) downstream))

(defn start-app [{:keys [starter started]}]
  (let [me (.getId (Thread/currentThread))]
    (println me "getting app")
    (if (= me (swap! starter (fn [id] (or id me))))
      (do
        (println me "starting app...")
        (Thread/sleep 10000)
        (println me "app started!")
        (deliver started true))
      (do
        (println me "lost the race, waiting for start...")
        (deref started)))))

(defn app-connection [app in out]
  (let [me (.getId (Thread/currentThread))]
    (println me "app connection")
    (when (nil? (deref (:starter app)))
      (println me "app not running")
      (start-app app))
    (println me "app started?")
    (when (deref (:started app))
      (println me "proxying!")
      (create-client (partial proxy-streams in out) (:downstream app)))))

(defn create-app-server [port downstream]
  (let [app {:downstream downstream
             :starter (atom nil)
             :started (promise)}]
    (create-server (partial app-connection app) port)))

(defn -main [& args]
  ; (create-server echo-server 10000)
  ; (create-server (partial proxy-server 10001) 10000)
  (create-app-server 10000 10001)
  (println "server started."))

