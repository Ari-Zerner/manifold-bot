(ns manifold-bot.core
  (:require [clojure.core.async :as async]
            [manifold-bot.api :as api]
            [manifold-bot.strategies :as strategies]))

;; Market data functions
(defn search-markets [query-params]
  (api/request "/search-markets" :get
               {:query-params (merge {:term ""} query-params)}))

;; Trading functions
(defn execute-trade [{:keys [market-id amount outcome limit duration-seconds dry-run] :as trade}]
  (api/request "/bet" :post
               {:body {:contractId market-id
                       :amount amount
                       :outcome outcome
                       :limitProb limit
                       :expiresAt (-> duration-seconds (* 1000) (+ (System/currentTimeMillis)))
                       :dryRun dry-run}})) 

(defn run-strategy [strategy]
  (doall
   (for [trade (->> strategy
                    strategies/get-search-params
                    search-markets
                    (strategies/get-trades strategy))]
    (let [{:keys [betId contractId orderAmount outcome] :as result} (execute-trade trade)]
      (println "Strategy" (:name strategy) "executed trade" betId "on market" contractId "for" orderAmount outcome)
      result))))

;; Main loop
(defn trading-loop []
  (async/go-loop []
    (doseq [strategy (strategies/get-strategies)]
      (run-strategy strategy))
    (async/<! (async/timeout (* 1000 (:poll-interval-seconds api/config))))
    (recur)))

;; Entry point
(defn -main [& args]
  (println "Starting Manifold trading bot...")
  (trading-loop)
  (println "Bot is running. Press Ctrl+C to stop.")
  (async/<!! (async/chan))) ; Block indefinitely