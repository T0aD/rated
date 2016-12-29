(ns rated.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
	    [cheshire.core :as json]
      [clojure.core.async :as async]
      [clojure.java.io :as io]
;      [clojure.tools.logging :as log]
;      [org.httpkit.server :refer [run-server]]
))
(require '[rated.buckets :as buckets])
(require '[rated.lifesaver :as lifesaver])

(defn http_response [hm]
  "Generates the default hash-map of a HTTP response"
  (merge {:status 200 :headers {"Content-Type" "application/json"}} hm))

(defn json_response [hm]
  (let [response (http_response {:body (json/generate-string hm)})]
     (println "response sent" response)
     response
  ))

(defn http_get [req]
  (println "received request" (:request-method req) (:uri req))
  ;(print "synced:" @buckets/synced? "buckets is" @buckets/buckets))
  (json_response {:name "Sexy API" :status "Sexy"})
)

(defn put_queue [name]
  (let [status (buckets/add_queue name)]
    (println "status is" status))
  )

(defn http_put [req]
  (println "put received")
  (put_queue "pierrot")
  (json_response {:status "OK"}))

; https://learnxinyminutes.com/docs/compojure/
(defroutes app-routes
;  (GET "/" [] "Hello World")
  (GET "/" [] http_get)
  (DELETE "/" [] "DELETE")
  (POST "/" [] "POST")
  (PUT "/" [] http_put)
  (route/not-found "Not Found"))

(defn init []
  (println "starting app...")
  (lifesaver/init)
  (buckets/init)
  (wrap-defaults app-routes site-defaults))

(def app
  (init))
