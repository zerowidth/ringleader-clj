(ns ringleader.core
  (:gen-class))

(defn -main [& args]
  (println "hello" (apply str (interpose "\n" args))))
