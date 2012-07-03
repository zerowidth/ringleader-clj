(ns ringleader.util)

; TODO make this a macro that takes a body of statements rather than a function
(defn on-thread [f]
  "Fire and forget code to run on a thread."
  (doto (Thread. ^Runnable f)
    (.start)))

