(ns notch.remotes.bodymedia
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as str])
  (:require [notch.remotes.oauth :as oauth] :reload)
  )

(defn- http-get
  "Helper function for OAuth HTTP Gets"
  [consumer access_token path & [query_params] ]

  (let [ ;;BodyMedia (Mashery) requires oauth consumer to be passed in as a query string
         query_params (merge query_params {:api_key (:oauth_consumer_key consumer)})]
    (-> (oauth/get (str (:base_uri consumer) path) {:consumer consumer
                                                    :token access_token
                                                    :query-params query_params})
      (json/read-str :key-fn keyword))

    ))

(defn access-token-expired?
  "Is the token expiration past the current time?"
  [access_token]
  (< (* 1000 (Long/parseLong (:xoauth_token_expiration_time access_token)))
    (System/currentTimeMillis)))

(defn get-user [oauth_consumer access_token]
  (http-get oauth_consumer access_token "/user/info"))



(defn get-user-last-sync
  "Get the last time the user synced their armband"
  [oauth_consumer access_token]
  (http-get oauth_consumer access_token "/user/sync/last" {}))

(defn get-weight-measurements
  "List the user's weights over time"
  [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/measurement/WEIGHT" {})
    :weight))

(defn get-personal-records
  "List the user's personal bests"
  [oauth_consumer access_token]
  (http-get oauth_consumer access_token "/notification/record" {}))

(defn get-current-preferences
  "List the user's current preferences"
  [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/preference/current" {})
    :preference))

(defn get-burn-days
  "Lists the user's caloric burn, per day, for the date range"
  [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/burn/day/intensity/" start_date "/" stop_date ) {})
    :days))

(defn get-burn-minutes
  "Lists the user's caloric burn, per day, for the date range"
  [oauth_consumer access_token date]
  (-> (http-get oauth_consumer access_token (str "/burn/day/minute/intensity/" date ) {})
    :days
    (first)
    :minutes))

(defn get-sleep-days
  "Lists the user's sleep, per day, for the date range"
  [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/sleep/day/" start_date "/" stop_date ) {})
    :days))

(defn get-step-days
  "Lists the user's steps, per day, for the date range"
  [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/step/day/" start_date "/" stop_date ) {})
    :days))

(defn get-step-hours
  "Lists the user's steps, per hour, for the date range"
  [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/step/day/hour/" start_date "/" stop_date ) {})
    :days))

(defn get-summaries
  "Lists the user's summaries for the date range"
  [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/summary/day/" start_date "/" stop_date ) {})
    identity))
