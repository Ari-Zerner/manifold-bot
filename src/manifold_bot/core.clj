(ns manifold-bot.core
  (:require [clojure.core.async :as async]
            [manifold-bot.config :as config]
            [manifold-bot.api :as api]
            [manifold-bot.strategies :as strategies]))

(defn search-markets
  "Searches for markets based on the given query parameters.
   
   Parameters:
   - query-params: A map of query parameters to filter the search results. See: https://docs.manifold.markets/api#get-v0search-markets
   
   Returns:
   A list of markets matching the query parameters."
  [query-params]
  (api/request "/search-markets" :get
               {:query-params (merge {:term ""} query-params)}))

(defn execute-trade
  "Executes a trade based on the given trade parameters.
   
   Parameters:
   - trade: A map containing trade details (market-id, amount, outcome, limit, duration-seconds, dry-run).
   
   Returns:
   The result of the trade execution."
  [{:keys [market-id amount outcome limit duration-seconds dry-run] :as trade}]
  (api/request "/bet" :post
               {:body {:contractId market-id
                       :amount amount
                       :outcome outcome
                       :limitProb limit
                       :expiresAt (-> duration-seconds (* 1000) (+ (System/currentTimeMillis)))
                       :dryRun (or dry-run false)}}))

; TODO use an actual logging framework
(defn report
  "Logs a message with a timestamp.
   
   Parameters:
   - args: Arguments to be logged, as with println."
  [& args]
  (let [time (.format 
               (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss ")
               (java.time.LocalDateTime/now))]
    (apply println time args)))

(defn run-strategy
  "Runs a given strategy by searching for markets and executing trades.
   
   Parameters:
   - strategy: A Strategy as defined by strategies/defstrategy.
   
   Returns:
   A list of executed trade results."
  [strategy]
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

(defn trading-loop
  "Starts the main trading loop, which runs strategies at regular intervals.
   
   Returns:
   A core.async channel that runs the trading loop."
  []
  (async/go-loop []
    (report "Balance:" (:balance (api/get-my-user-info)))
    (dotimes [_ (config/polls-per-report)]
      (doseq [strategy (strategies/get-strategies)]
        (run-strategy strategy))
      (async/<! (async/timeout (* 1000 (config/poll-interval-seconds)))))
    (recur)))

(defn -main
  "Entry point for the trading bot.
   Starts the trading loop and keeps the program running."
  [& args]
  (report "Enabled strategies:" (clojure.string/join ", " (map :name (strategies/get-strategies))))
  (trading-loop)
  (report "Bot is running. Press Ctrl+C to stop.")
  (async/<!! (async/chan))) ; Block indefinitely