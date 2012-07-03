(ns ringleader.util)

(defmacro on-thread [& body]
  "Execute body on its own thread. Provides an implicit (do ...)."
  (when body
    `(doto (Thread. ^Runnable #(do ~@body)) (.start))))

