(ns rated.lifesaver)

; this module is used to report when an essential thread dies
; and takes action accordingly (you can implement a fail fast system and exit()) ASAP
; {"tickname" {:maxinterval :timeout :action}}
(def ticks (atom {}))

(defn now []
    (int (/ (System/currentTimeMillis) 1000)))

(defn alerter [mentry]
  (let [name (key mentry) value (val mentry) current (now)
      delta (- current value)]
    (if (> delta 10)
      (do
        (println (format "lifesaver-alerter: %s tick last received %d sec ago" name delta))
        (println "aborting process....")
        (System/exit 1)
      ))
  ))

(defn supervisor []
  "Sole role is to loop over ticks and checks out which ones"
  "weren't updated for a long time"
  (loop []
    ;(println "lifesaver: checking ticks")
    (mapv alerter @ticks)
    (Thread/sleep 1000)
    (recur))
  )

(defn update-tick [name]
  "Updates a specific tick"
  (swap! ticks assoc name (now)))

(defn init []
  (println "rated.lifesaver started")
    (future (supervisor)))
