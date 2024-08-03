(ns manifold-bot.strategies 
  (:require [manifold-bot.api :as api]
            [manifold-bot.config :as config]))

;; Registry for storing strategies
(def strategy-registry (atom {}))

;; Macro for defining strategies
(defmacro defstrategy [name & {:keys [description get-search-params get-trades]}]
  (let [enabled (:enabled (config/config-for-strategy name))
        strategy {:name (str name)
                  :description description
                  :get-search-params get-search-params
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
  ((:get-search-params strategy) (config/config-for-strategy (:name strategy))))

(defn get-trades
  [strategy markets]
  ((:get-trades strategy) (config/config-for-strategy (:name strategy)) markets))

;; Simple Strategy
(defstrategy Null
  :description "Do nothing"
  :get-search-params {:limit 1}
  :get-trades (fn [config markets] []))

(defstrategy CoinFlip
  :description "Market make in @Traveel's daily coinflip markets"
  :get-search-params (fn [config]
                       {:term "Daily coinflip"
                        :creatorId "gRmM27eQDEVTEjM1q6Yzc7abJT93" ; @Traveel
                        :filter "open"
                        :contractType "BINARY"})
  :get-trades (fn [config markets]
                (mapcat (fn [{:keys [id]}]
                          (let [positions (api/get-my-positions id)
                                orders (api/get-my-open-orders id)
                                should-bet? (fn [outcome]
                                              (not (or
                                                    ((keyword outcome) positions)
                                                    (some #(= (:outcome %) outcome) orders))))
                                duration-seconds (* 30 24 60 60)]
                            (remove nil?
                                    [(when (should-bet? "YES")
                                       {:market-id id
                                        :amount (:size config)
                                        :outcome "YES"
                                        :limit 0.47
                                        :duration-seconds duration-seconds})
                                     (when (should-bet? "NO")
                                       {:market-id id
                                        :amount (:size config)
                                        :outcome "NO"
                                        :limit 0.53
                                        :duration-seconds duration-seconds})])))
                        markets)))

;; Get all enabled strategies
(defn get-strategies []
  (vals @strategy-registry))