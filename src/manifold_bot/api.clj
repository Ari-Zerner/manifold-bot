(ns manifold-bot.api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [manifold-bot.config :as config]))

(defn jsonify-body [opts]
  (if (map? (:body opts))
    (merge opts {:content-type :json
                 :body (json/generate-string (:body opts))})
    opts))

;; API interaction
(defn request [endpoint method & [opts]]
  (let [url (str (config/api-base-url) endpoint)
        default-opts {:headers {"Authorization" (str "Key " (config/api-key))}
                      :as :json
                      :coerce :always}
        response (http/request (merge default-opts
                                      (jsonify-body opts)
                                      {:method method
                                       :url url}))]
    (:body response)))

;; Load user info
(def get-my-user-info (memoize (fn [] (request "/me" :get))))