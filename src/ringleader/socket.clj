(ns ringleader.socket
  (:require [clojure.java.io :as io])
  (:import (java.net InetAddress ServerSocket Socket SocketException)
           (java.io InputStreamReader OutputStreamWriter BufferedReader
                    BufferedWriter)))
; copied and tweaked from socket-server.clj
; each accepted connection runs on its own thread

(defn- on-thread [f]
  (doto (Thread. ^Runnable f)
    (.start)))

(defn proxy-streams [upstream-in upstream-out downstream-in downstream-out]
  (let [finished (promise)]
    (on-thread #(do
                  (try
                    (io/copy upstream-in downstream-out)
                    (catch SocketException e))
                  (deliver finished true)))
    (on-thread #(do
                  (try
                    (io/copy downstream-in upstream-out)
                    (catch SocketException e))
                  (deliver finished true)))
    ; wait for either the upstream *or* downstream to close
    (deref finished)))

(defn- close-socket [^Socket s]
  (when-not (.isClosed s)
    (doto s
      (.shutdownInput)
      (.shutdownOutput)
      (.close))))

(defn- accept-fn [^Socket s connections fun]
  (println "connection accepted")
  (let [in (.getInputStream s)
        out (.getOutputStream s)]
    (on-thread #(do
                  (dosync (commute connections conj s))
                  (try
                    (fun in out)
                    (catch SocketException e))
                  (println "closing connection")
                  (close-socket s)
                  (dosync (commute connections disj s))))))

(defn create-server [fun port]
  (let [server (ServerSocket. port)
        connections (ref #{})]
    (on-thread #(when-not (.isClosed server)
                  (try
                    (accept-fn (.accept server) connections fun)
                    (catch SocketException e))
                  (recur)))
    {:socket server :connections connections}))

(defn close-server [server]
  (doseq [s @(:connections server)]
    (close-socket s))
  (dosync (ref-set (:connections server) #{}))
  (.close ^ServerSocket (:server-socket server)))

(defn connection-count [server]
  (count @(:connections server)))

(defn- open-connection [host port]
  (try
    (Socket. host port)
    (catch SocketException e
      (println "could not connect to" host "on port" port))))

(defn create-client [fun port]
  (if-let [s (open-connection "localhost" port)]
    (let [in (.getInputStream s)
          out (.getOutputStream s)]
      (try
        (fun in out)
        (catch SocketException e))
      (println "closing client connection")
      (close-socket s))
    (println "couldn't connect?")))


