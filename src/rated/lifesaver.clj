(ns rated.lifesaver)

; this module is used to report when an essential thread dies
; and takes action accordingly (you can implement a fail fast system and exit()) ASAP

(def ticks (atom {}))

(defn supervisor []
  "Sole role is to loop over ticks and checks out which ones"
  "weren't updated for a long time"
  (loop []
    (println "lifesaver: checking ticks")
    (Thread/sleep 1000)
    (recur))
  )

(defn init []
  (println "rated.lifesaver started")
  (future supervisor)
  )
