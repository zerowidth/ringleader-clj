(ns ringleader.socket
  (:import (java.net InetAddress ServerSocket Socket SocketException)
           (java.io InputStreamReader OutputStreamWriter BufferedReader
                    BufferedWriter)))
; copied and tweaked from socket-server.clj
; each accepted connection runs on its own thread

(defn- on-thread [f]
  (doto (Thread. ^Runnable f)
    (.start)))

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
                    (fun s in out)
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

