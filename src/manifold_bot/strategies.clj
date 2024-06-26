(ns manifold-bot.strategies)

;; Registry for storing strategies
(def strategy-registry (atom {}))

;; Macro for defining strategies
(defmacro defstrategy [name & {:keys [enabled description search-params get-trades]}]
  (let [strategy {:name (str name)
                  :description description
                  :search-params search-params
                  :get-trades get-trades}
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
  :search-params {:limit 0}
  :get-trades (fn [markets] []))

;; Get all enabled strategies
(defn get-strategies []
  (vals @strategy-registry))