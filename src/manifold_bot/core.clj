(ns manifold-bot.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :as async]
            [clojure.edn :as edn]
            [manifold-bot.strategies :as strategies]))

;; Configuration
(def config (edn/read-string (slurp "config.edn")))

;; API interaction
(defn api-request [endpoint method & [opts]]
  (let [url (str (:api-base-url config) endpoint)
        default-opts {:headers {"Authorization" (str "Key " (:api-key config))}
                      :as :json
                      :coerce :always}
        response (http/request (merge default-opts
                                      opts
                                      {:method method
                                       :url url}))]
    (:body response)))

;; Load user info
(def get-my-user-info (memoize (fn [] (api-request "/me" :get))))

;; Market data functions
(defn search-markets [query-params]
  (api-request "/search-markets" :get {:query-params query-params}))

;; Trading functions
(defn execute-trade [{:keys [market-id amount outcome limit duration-seconds] :as trade}]
  (->
   (api-request "/bet" :post
               {:form-params {:contractId market-id
                              :amount amount
                              :outcome outcome
                              :limitProb limit
                              :expiresAt (cond-> duration-seconds (* 1000) (+ (System/currentTimeMillis)))}})
   :betId))

;; Main loop
(defn trading-loop []
  (async/go-loop []
    (doseq [strategy (strategies/get-strategies)
            trade (->> strategy
                        strategies/get-search-params
                        search-markets
                        (strategies/get-trades strategy))]
      (let [bet-id (execute-trade trade)]
        (println "Strategy " (:name strategy) " executed trade " bet-id)))
    (async/<! (async/timeout (* 1000 (:poll-interval-seconds config))))
    (recur)))

;; Entry point
(defn -main [& args]
  (println "Starting Manifold trading bot...")
  (trading-loop)
  (println "Bot is running. Press Ctrl+C to stop.")
  (async/<!! (async/chan))) ; Block indefinitely