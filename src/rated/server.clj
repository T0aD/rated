(ns rated.server (:gen-class)
  (:require [org.httpkit.server :as httpkit]
            [rated.handler :refer [app]]
            [clojure.tools.cli :as cli]
            ;[api.mongo-adapter :as mongo]
            [rated.lifesaver :as lifesaver]
            [rated.buckets :as buckets]
  ))

(def cli-options
[["-p" "--port PORT" "Port number" :default 3000
  :parse-fn #(Integer/parseInt %)
  :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65535"]]
  [nil "--gc-interval INT" "Interval in seconds of garbage collector"]
  [nil "--persistor-interval INT" "Interval in between to check for persistency of data"]
  [nil "--mongo-uri STRING" "MongoDB address"
  :default "mongodb://m3.vigiglo.be:31592/spritz"]])

(defonce server (atom nil))

(defn -main [& args]
  (let [opts (cli/parse-opts args cli-options)]
    (when (:errors opts)
      (binding [*out* *err*]
        (println (clojure.string/join "\n" (:errors opts)))
        (println (:summary opts)))
      (System/exit 1))
    (let [{:keys [mongo-uri port]} (:options opts)]
      ;(mongo/init-mongo mongo-uri)
      (lifesaver/init)
      (buckets/init)
      (println "ABOUT TO LAUNCH DAS SERVER!")
      (reset! server (httpkit/run-server #'app {:port (or port 3000)})))))

(defn stop! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start! []
  (reset! server (httpkit/run-server #'app)))
