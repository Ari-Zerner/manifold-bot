(ns manifold-bot.api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [manifold-bot.config :as config]))

(defn jsonify-body
  "Converts the :body of the options map to JSON if it's a map.
   
   Parameters:
   - opts: A map containing request options.
   
   Returns:
   The options map with :content-type set to :json and :body converted to a JSON string if applicable."
  [opts]
  (if (map? (:body opts))
    (merge opts {:content-type :json
                 :body (json/generate-string (:body opts))})
    opts))

(defn request
  "Makes an HTTP request to the Manifold API.
   
   Parameters:
   - endpoint: The API endpoint to request.
   - method: The HTTP method to use (:get, :post, etc.).
   - opts: (optional) A map of additional request options.
   
   Returns:
   The parsed JSON response body."
  [endpoint method & [opts]]
  (let [url (str (config/api-base-url) endpoint)
        default-opts {:headers {"Authorization" (str "Key " (config/api-key))}
                      :as :json
                      :coerce :always}
        response (http/request (merge default-opts
                                      (jsonify-body opts)
                                      {:method method
                                       :url url}))]
    (:body response)))

(defn get-my-user-info
  "Retrieves the bot's information from the Manifold API.
   
   Returns:
   A map containing the user's information."
  []
  (request "/me" :get))

(def my-user-id
  "The user ID of the bot, retrieved from the API."
  (:id (get-my-user-info)))

(defn get-my-open-orders
  "Retrieves the bot's open orders for a specific market.
   
   Parameters:
   - market-id: The ID of the market to query.
   
   Returns:
   A list of open orders for the specified market."
  [market-id]
  (request
   "/bets" :get
   {:query-params {:contractId market-id
                   :userId my-user-id
                   :kinds "open-limit"}}))

(defn get-my-positions
  "Retrieves the bot's positions for a specific market.
   
   Parameters:
   - market-id: The ID of the market to query.
   
   Returns:
   A map of outcomes to total shares held by the user."
  [market-id]
  (->> (request
        (str "/market/" market-id "/positions") :get
        {:query-params {:userId my-user-id}})
       (map :totalShares)
       (apply merge-with +)))