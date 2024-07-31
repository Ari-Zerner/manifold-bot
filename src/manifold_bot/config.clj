(ns manifold-bot.config
  (:require [clojure.edn :as edn]))

;; Configuration
(def config (edn/read-string (slurp "config.edn")))

(defn api-key []
  (:api-key config))

(defn api-base-url []
  (:api-base-url config))

(defn poll-interval-seconds []
  (:poll-interval-seconds config))
