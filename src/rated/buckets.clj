(ns rated.buckets
  (:require [clojure.java.io :as io]
            [rated.lifesaver :as lifesaver]
))

; default configuration
(def DEFAULT_TTL 60)
(def DEFAULT_LEN 10)

; definitions of atoms
(def buckets (atom {}))
(def synced? (atom true))

(defn unsync []
  (if @synced?
    (do
      (reset! synced? false)
      true
      )
      false))

; DELETE a bucket
(defn delete_bucket [name]
  (println "-" name)
  (if (nil? (get @buckets name))
    (println "bucket" name "does not exist!")
  )
  (swap! buckets dissoc name)
)

; CREATE or UPDATE a bucket
(defn create_bucket [name len ttl]
  (println "+" name {:ttl ttl :len len})
  (if (contains? @buckets name)
      ; if bucket already exists, keeps its current queue
      (swap! buckets (fn [bs] (assoc bs name {:ttl ttl :len len :queue (get-in bs [name :queue])})))
      ; brand new bucket
      (swap! buckets assoc name {:ttl ttl :len len :queue []})
    )
)
(defn post_bucket [name doc]
  (println "POSTed on bucket" name)
  (let [ttl (or (get doc :ttl) DEFAULT_TTL)
        len (or (get doc :len) DEFAULT_LEN)]
	(do
	 (println "calling create_bucket" name "ttl=" ttl "len=" len)
	 (create_bucket name len ttl)
   (println "unsyncing:" (unsync))
;   (reset! synced? false)
	 )
	)
)

; return current timestamp and adds a delta
(defn now []
  (int (/ (System/currentTimeMillis) 1000)))
(defn now-ms []
 (System/currentTimeMillis))
(defn ts
  ([] (ts 0))
  ([delta] (+ delta (now))))
(defn delta [old-timestamp]
  (- (now-ms) old-timestamp))


; try to add an element to a queue and return a status atom
(defn add_queue_item [queue timestamp]
  (conj queue {:timestamp timestamp}))


(defn add_queue [name]
  (let [status (atom :ok)]
    (swap! buckets
      (fn [bs]
        (if (nil? (get bs name))
          (do
;            (println name "doesnt exist!")
            (reset! status :none))
        (let [ttl (get-in bs [name :ttl])
            len (get-in bs [name :len])
            queue (get-in bs [name :queue])
            timestamp (ts ttl)]
;        (println "ttl for bucket" name "is" ttl)
;        (println "timestamp generated" (now) timestamp (- timestamp (now)))
;        (println (format "bucket %s queue: %d/%d" name (count queue) len))
        ; the magic is here
        (if (<= len (count queue))
          (do ;(println "limit reached!")
            ; how to notify caller with return value
            (reset! status :no)
            ; return unchanged buckets hashmap
            bs)
          (do ;(println "free room ! welcome!")
            ; insert new item in queue and returns modified bucket
            (assoc-in bs [name :queue] (add_queue_item queue timestamp))
          ))
          ))))
        @status)
)

(defn map-vals [f hm] (into {} (mapv (fn [[k v]] [k (f v)]) hm)))

; remove entries with old timestamps from queues of this bucket
(defn old? [current item]
  (> current (:timestamp item))
)

(defn clean_queue [queue]
  (let [current (now)
        new_queue (remove (partial old? current) queue)]
        (let [new_count (count new_queue) old_count (count queue)
            delta (- old_count new_count)]
            (when (> delta 0)
              (println "gc removed" delta "entries")
              (reset! synced? false)
            ))
        new_queue))

(defn clean_buckets [buckets]
  (do
    (map-vals (fn [bs]
      (assoc bs :queue (clean_queue (:queue bs)))) buckets)
  ))


(defn garbage_collector [interval]
  (println "garbage collector started")
  (loop [x 0]
    (lifesaver/update-tick "buckets/gc")
    ;(println "--> gc up" x)
    ;(mapv (fn [[name bucket]] (clean_queues name bucket)) @buckets)
    (swap! buckets clean_buckets)
    ;(println "<-- gc down")
    (Thread/sleep (* 1000 interval))
    (recur x)))
    #_(if (< x 20)
      (recur (inc x))
      (println "gc ended" @buckets))

; could be move to final lib with mongo/mysql dependency...:
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

(defn dontimes [times expression]
  (println "about to execute" times "times the following" expression)
  (loop [x 0]
    (let [wait (rand-int 100)]
      (Thread/sleep wait)
      ;(println "executed" expression "after" wait "ms")
      (eval expression))
      (if (<= x times) (recur(inc x)))))


(defn init []
  (println "buckets.clj init")
  (future (persistor 4))
  (future (garbage_collector 1))

  (post_bucket "pierrot" {:len 20 :ttl 16})
  (post_bucket "toad" {:len 20 :ttl 18})
  ;(future (dontimes 100 '(rated.buckets/add_queue "pierrot")))
  ;(future (dontimes 200 '(rated.buckets/add_queue "toad")))
  (println "buckets.clj end-of-init"))
