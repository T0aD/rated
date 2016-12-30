; this will be the module to insure persistency of database

(def runs (atom 0))
(defn persistor [interval]
  (def last-persist (atom (now-ms)))
  (loop []
    (lifesaver/update-tick "buckets/persistor")
    ;(swap! runs inc)
;    (println "persistor-thread: synced?" @synced? "since" (delta @last-persist) "ms")
    (if (not @synced?)
      (do
        (println "syncing after" (delta @last-persist) "ms")
        (reset! synced? true)
        (reset! last-persist (now-ms))
        )
      )
    (Thread/sleep (* 1000 interval))
    (recur)
    #_(if (< @runs 20)
      (recur)
      (println "persistor ended"))
  ))
