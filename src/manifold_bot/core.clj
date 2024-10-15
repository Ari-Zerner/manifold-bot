(ns manifold-bot.core
  (:require [clojure.core.async :as async]
            [manifold-bot.config :as config]
            [manifold-bot.api :as api]
            [manifold-bot.strategies :as strategies]
            [clojure.tools.logging :as log]))

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
   - trade: A map containing trade details (market-id, amount, outcome, limit, duration-seconds (optional), dry-run).
   
   Returns:
   The result of the trade execution."
  [{:keys [market-id amount outcome limit duration-seconds dry-run] :as trade}]
  (api/request "/bet" :post
               {:body {:contractId market-id
                       :amount amount
                       :outcome outcome
                       :limitProb limit
                       :expiresAt (or (some-> duration-seconds (* 1000) (+ (System/currentTimeMillis))) (:closeTime (api/request (str "/market/" market-id) :get)))
                       :dryRun (or dry-run false)}}))

(defn run-strategy
  "Runs a given strategy by searching for markets and executing trades.
   
   Parameters:
   - strategy: A Strategy as defined by strategies/defstrategy.
   
   Returns:
   A list of executed trade results."
  [strategy]
  (try
    (doall
     (for [search-params (strategies/get-search-params strategy)
           trade (->> search-params
                      search-markets
                      (strategies/get-trades strategy))]
       (try
         (let [{:keys [betId contractId orderAmount outcome fills probBefore probAfter] :as result} (execute-trade trade)
               effect (if fills
                        (str "(" (->> fills (map :shares) (reduce +)) " filled)")
                        (str "(" probBefore "% -> " probAfter "%)"))]
           (log/info "Strategy" (:name strategy) "executed trade" betId "on market" contractId "for" orderAmount outcome effect)
           result)
         (catch Exception e
           (log/error "Error executing trade for strategy" (:name strategy) ":" (.getMessage e))
           nil))))
    (catch Exception e
      (log/error "Error running strategy" (:name strategy) ":" (.getMessage e))
      [])))

(defn trading-loop
  "Starts the main trading loop, which runs strategies at regular intervals.
   
   Returns:
   A core.async channel that runs the trading loop."
  []
  (async/go-loop []
    (try
      (let [user-info (api/get-my-user-info)
            balance (:balance user-info)
            net-worth (+ (:totalDeposits user-info) (get-in user-info [:profitCached :allTime]))]
        (log/info "Balance:" balance "\tNet worth:" net-worth)) ; TODO account for fees in net worth
      (catch Exception e
        (log/error "Error fetching balance:" (.getMessage e))))
    (try
      (dotimes [_ (config/polls-per-report)]
        (doseq [strategy (strategies/get-strategies)]
          (run-strategy strategy))
        (async/<! (async/timeout (* 1000 (config/poll-interval-seconds)))))
      (catch Exception e
        (log/error "Error in trading loop:" (.getMessage e))))
    (recur)))

(defn -main
  "Entry point for the trading bot.
   Starts the trading loop and keeps the program running."
  [& args]
  (log/info "Enabled strategies:" (clojure.string/join ", " (map :name (strategies/get-strategies))))
  (trading-loop)
  (log/info "Bot is running. Press Ctrl+C to stop.")
  (async/<!! (async/chan))) ; Block indefinitely