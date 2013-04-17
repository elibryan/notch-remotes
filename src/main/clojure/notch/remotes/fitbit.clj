(ns notch.remotes.fitbit
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as str])
  (:require [notch.remotes.oauth :as oauth] :reload)
  )

(defn- http-get
  "Helper function for OAuth HTTP Gets"
  [consumer access_token path & [query_params] ]

  (-> (oauth/get (str (:base_uri consumer) path) {:consumer consumer
                                                        :token access_token
                                                        :query-params query_params})
      (json/read-str :key-fn keyword))
  )


(defn get-user [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/1/user/-/profile.json")
    :user
    )
  )

(defn get-steps-series [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/activities/steps/date/" start_date "/" stop_date ".json"))
    :activities-steps
    ))

(defn get-steps-intraday [oauth_consumer access_token date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/activities/steps/date/" date "/1d.json"))
    :activities-steps-intraday :dataset
    ))

(defn get-calories-series [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/activities/tracker/calories/date/" start_date "/" stop_date ".json"))
    :activities-tracker-calories
    ))

(defn get-weight-series [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/body/weight/date/" start_date "/" stop_date ".json"))
    :body-weight
    ))

(defn get-calories-in-series [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/foods/log/caloriesIn/date/" start_date "/" stop_date ".json"))
    :foods-log-caloriesIn
    ))

(defn get-water-in-series [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/foods/log/water/date/" start_date "/" stop_date ".json"))
    :foods-log-water
    ))

(defn get-sleep-series-start-time [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/sleep/startTime/date/" start_date "/" stop_date ".json"))
    :sleep-startTime
    ))

(defn get-sleep-series-time-in-bed [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/sleep/timeInBed/date/" start_date "/" stop_date ".json"))
    :sleep-timeInBed
    ))

(defn get-sleep-series-minutes-asleep [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/sleep/minutesAsleep/date/" start_date "/" stop_date ".json"))
    :sleep-minutesAsleep
    ))


(defn get-steps
  "get events from [start_date to stop_date)
  start_date and stop_date are YYYY-MM-DD format"
  [consumer access_token start_date stop_date & [{intra_day :intra_day}]]
  (let [steps_series (get-steps-series consumer access_token start_date stop_date)]
    (if intra_day
      (pmap #(assoc % :intra (get-steps-intraday consumer access_token (:dateTime %))) steps_series)
      steps_series
      )
  ))

