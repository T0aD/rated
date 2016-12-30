(ns rated.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
	          [cheshire.core :as json]
            [clojure.java.io :as io]
            [org.httpkit.server :as httpkit]
            [clojure.tools.cli :as cli]
            [rated.lifesaver :as lifesaver]
            [rated.buckets :as buckets]
;            [api.mongo-adapter :as mongo]
      ;      [clojure.core.async :as async]
;      [clojure.tools.logging :as log]
;      [org.httpkit.server :refer [run-server]]
))
;(require '[rated.buckets :as buckets])
;(require '[rated.lifesaver :as lifesaver])

(defn http_response [hm]
  "Generates the default hash-map of a HTTP response"
  (merge {:status 200 :headers {"Content-Type" "application/json"}} hm))

(defn json_response [hm]
  (let [response (http_response {:body (json/generate-string hm)})]
     ;(println "response sent" response)
     response
  ))

(defn http_get [req]
  ;(println "received request" (:request-method req) (:uri req))
  ;(print "synced:" @buckets/synced? "buckets is" @buckets/buckets))
  (json_response {:name "Sexy API" :status "Sexy"})
)

(defn pre [expr]
  (println expr)
  expr)

(defn put_queue [name]
  (let [status (buckets/add_queue name)]
    (println "putting into" name)
    ;(println "status is" status)
    (case status
      :ok   (http_response {:status 200 :body ""})
      :no   (http_response {:status 429 :body (json/generate-string {:state "full"})})
      :none (http_response {:status 404}))))

(defn http_put [name]
  (put_queue name))

(defn post_bucket [name]
  (buckets/post_bucket name {:ttl 3 :len 10})
  (http_response {}))

(defn del_bucket [name]
  (buckets/delete_bucket name)
  ;(println @buckets/buckets)
  (http_response {:status 200}))


; https://learnxinyminutes.com/docs/compojure/
(defroutes app-routes
;  (GET "/" [] "Hello World")
  (GET "/" [] http_get)
  (DELETE "/:name" [name] (del_bucket name))
  (POST "/:name" [name] (post_bucket name))
  (PUT "/:name" [name] (http_put name))
  (route/not-found "Not Found"))


(defn init []
  ;(println "starting app...")
  ;(lifesaver/init)
  ;(buckets/init)
  (wrap-defaults app-routes site-defaults))

(def app
  app-routes)
;  (wrap-defaults app-routes site-defaults))

#_(def app
  (init))
