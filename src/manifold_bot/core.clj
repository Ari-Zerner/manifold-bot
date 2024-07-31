(ns manifold-bot.core
  (:require [clojure.core.async :as async]
            [manifold-bot.config :as config]
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
                       :dryRun (or dry-run false)}}))

; TODO use an actual logging framework
(defn report [& args]
  (let [time (.format 
               (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss ")
               (java.time.LocalDateTime/now))]
    (apply println time args)))

(defn run-strategy [strategy]
  (try
    (doall
     (for [trade (->> strategy
                      strategies/get-search-params
                      search-markets
                      (strategies/get-trades strategy))]
       (try
         (let [{:keys [betId contractId orderAmount outcome] :as result} (execute-trade trade)]
           (report "Strategy" (:name strategy) "executed trade" betId "on market" contractId "for" orderAmount outcome)
           result)
         (catch Exception e
           (report "Error executing trade for strategy" (:name strategy) ":" (.getMessage e))
           nil))))
    (catch Exception e
      (report "Error running strategy" (:name strategy) ":" (.getMessage e))
      [])))

;; Main loop
(defn trading-loop []
  (async/go-loop []
    (report "Balance:" (:balance (api/get-my-user-info)))
    (dotimes [_ (config/polls-per-report)]
      (doseq [strategy (strategies/get-strategies)]
        (run-strategy strategy))
      (async/<! (async/timeout (* 1000 (config/poll-interval-seconds)))))
    (recur)))

;; Entry point
(defn -main [& args]
  (report "Enabled strategies:" (clojure.string/join ", " (map :name (strategies/get-strategies))))
  (trading-loop)
  (report "Bot is running. Press Ctrl+C to stop.")
  (async/<!! (async/chan))) ; Block indefinitely