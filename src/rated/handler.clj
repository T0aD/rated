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

(defn json_response [hm]
  (let [response { :status 200
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string hm)}]
     (println "response is" response)
     response
  ))

(defn http_get [req]
  (println "received request" (:request-method req) (:uri req))
  ;(print "synced:" @buckets/synced? "buckets is" @buckets/buckets))
  (json_response {:name "Sexy API" :status "Sexy"})
)

; https://learnxinyminutes.com/docs/compojure/
(defroutes app-routes
;  (GET "/" [] "Hello World")
  (GET "/" [] http_get)
  (DELETE "/" [] "DELETE")
  (POST "/" [] "POST")
  (PUT "/" [] "PUT")
  (route/not-found "Not Found"))

(defn init []
  (println "starting app...")
  (buckets/init)
  (wrap-defaults app-routes site-defaults))

(def app
  (init))
