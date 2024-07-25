(ns manifold-bot.strategies 
  (:require [manifold-bot.api :as api]))

;; Registry for storing strategies
(def strategy-registry (atom {}))

;; Macro for defining strategies
(defmacro defstrategy [name & {:keys [enabled description search-params get-trades]}]
  (let [strategy {:name (str name)
                  :description description
                  :search-params search-params
                  :get-trades (if enabled
                                get-trades
                                `(fn [markets#]
                                   (map #(assoc % :dry-run true)
                                        (~get-trades markets#))))}
        sym (symbol (str (clojure.string/lower-case (str name)) "-strategy"))]
    `(do
       (def ~sym ~strategy)
       (when ~enabled
         (swap! strategy-registry assoc '~name ~strategy))
       ~strategy)))

(defn get-search-params
  [strategy]
  (:search-params strategy))

(defn get-trades
  [strategy markets]
  ((:get-trades strategy) markets))

;; Simple Strategy
(defstrategy Null
  :enabled false
  :description "Do nothing"
  :search-params {:limit 1}
  :get-trades (fn [markets] []))

(defstrategy CoinFlip
  :enabled true
  :description "Market make in @Traveel's daily coinflip markets"
  :search-params {:term "Daily coinflip"
                  :creatorId "gRmM27eQDEVTEjM1q6Yzc7abJT93" ; @Traveel
                  :filter "open"
                  :contractType "BINARY"}
  :get-trades (fn [markets]
                (let [user-id (:id (api/get-my-user-info))
                      get-user-bets (fn [market-id]
                                      (api/request
                                       "/bets" :get
                                       {:query-params {:contractId market-id
                                                       :userId user-id}}))
                      has-position-or-order? (fn [bets outcome]
                                               (some #(= (:outcome %) outcome) bets))]
                  (mapcat (fn [market]
                            (let [user-bets (get-user-bets (:id market))]
                              (remove nil?
                                      [(when-not (has-position-or-order? user-bets "YES")
                                         {:market-id (:id market)
                                          :amount 10
                                          :outcome "YES"
                                          :limit 0.47
                                          :duration-seconds 86400})
                                       (when-not (has-position-or-order? user-bets "NO")
                                         {:market-id (:id market)
                                          :amount 10
                                          :outcome "NO"
                                          :limit 0.53
                                          :duration-seconds 86400})])))
                          markets))))

;; Get all enabled strategies
(defn get-strategies []
  (vals @strategy-registry))