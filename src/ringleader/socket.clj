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

(defn proxy-streams
  [upstream-in upstream-out downstream-in downstream-out]
  "Proxy a pair of in/out streams to another pair of in/out streams."
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

(defn close-socket [^Socket s]
  (when-not (.isClosed s)
    (doto s
      (.shutdownInput)
      (.shutdownOutput)
      (.close))))

(defn- accept-fn [^Socket s connections callback]
  (println "connection accepted")
  (let [in (.getInputStream s)
        out (.getOutputStream s)]
    (on-thread #(do
                  (dosync (commute connections conj s))
                  (try
                    (callback in out)
                    (catch SocketException e))
                  (println "closing connection")
                  (close-socket s)
                  (dosync (commute connections disj s))))))

(defn create-server
  [callback port]
  "Create a socket server.

   callback  - callback function for each new connection. Called with the input
               and output streams for the connected socket. Runs in its own
               thread, and the socket is closed when this function returns.
   port      - what port the socket listens on"

  (let [server (ServerSocket. port)
        connections (ref #{})]
    (on-thread #(when-not (.isClosed server)
                  (try
                    (accept-fn (.accept server) connections callback)
                    (catch SocketException e))
                  (recur)))
    {:socket server :connections connections}))

(defn close-server [server]
  "Close a server and its connected clients

   server - a server map with the :server-socket and :connections to close out."
  (doseq [s @(:connections server)]
    (close-socket s))
  (dosync (ref-set (:connections server) #{}))
  (.close ^ServerSocket (:server-socket server)))

(defn connection-count [server]
  "Returns how many clients are connected to a server"
  (count @(:connections server)))

(defn open-connection [host port]
  "Open a connection to host/port and return it, or return nil if unsuccessful"
  (try
    (Socket. host port)
    (catch SocketException e
      (println "could not connect to" host "on port" port))))

(defn create-client [callback port]
  "Create a socket client.

  callback - connection callback, called with the in/out streams for a connected
             socket if the connection is successful. The socket connection is
             closed when this function returns.
  port     - what port to connect to"

  (if-let [s (open-connection "localhost" port)]
    (let [in (.getInputStream s)
          out (.getOutputStream s)]
      (try
        (callback in out)
        (catch SocketException e))
      (println "closing client connection")
      (close-socket s))
    (println "couldn't connect?")))

