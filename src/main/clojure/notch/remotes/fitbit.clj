(ns notch.remotes.fitbit
  (:use clojure.tools.logging)
  (:use notch.remotes.util :reload)
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

(defn get-sleep-series-minutes-awake [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/sleep/minutesAwake/date/" start_date "/" stop_date ".json"))
    :sleep-minutesAwake
    ))

(defn get-sleep-series-minutes-to-fall-asleep [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/sleep/minutesToFallAsleep/date/" start_date "/" stop_date ".json"))
    :sleep-minutesToFallAsleep
    ))

(defn get-sleep-series-minutes-after-wakeup [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/sleep/minutesAfterWakeup/date/" start_date "/" stop_date ".json"))
    :sleep-minutesAfterWakeup
    ))

(defn get-sleep-series-efficiency [oauth_consumer access_token start_date stop_date]
  (-> (http-get oauth_consumer access_token (str "/1/user/-/sleep/efficiency/date/" start_date "/" stop_date ".json"))
    :sleep-efficiency
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







(def ^:private request_date_formatter (org.joda.time.format.DateTimeFormat/forPattern "yyyy-MM-dd"))

(defn format-request-date
  "dt_string is an RFC3339 string
  this converts it into 'yyyy-MM-dd' for service calls"
  [dt_string]
  (.print request_date_formatter (parse-datetime (str dt_string ))))


(defn- parse-fitbit-datetime [date_string &[time_string]]
  (if time_string
    (datetime-with-tz-retain-fields (parse-datetime (str date_string "T" time_string )) "-00:00")
    (datetime-with-tz-retain-fields (parse-datetime (str date_string )) "-00:00")
    )
  )

(defn- format-fitbit-datetime [dt]
  (.print (get-datetime-formatter-3339 "-00:00") dt)
  #_(when date_string
    (str date_string
      (if (not (str/blank? time_string)) (str "T" time_string ":00.000-00:00" ) (str "T00:00:00.000-00:00")))))


(defn- parse-long [s]
  (try (long (Double/parseDouble s)) (catch Throwable ex nil)))

(defn- create-sleep-resp [start_time & args]
  (->> (apply conj [(:dateTime start_time) (:value start_time)] (map #(parse-long (:value %)) args))
    (zipmap [:date_time :start_timepart :minutes_in_bed :minutes_asleep :minutes_to_fall_asleep :minutes_after_wakeup :efficiency ])
  )
  )

(defn- get-sleep-start-time [{:keys [date_time start_timepart] :as resp}]
  (format-fitbit-datetime (parse-fitbit-datetime date_time start_timepart)))

(defn- get-sleep-end-time [{:keys [date_time start_timepart minutes_in_bed] :as resp}]
  (-> (parse-fitbit-datetime (get-sleep-start-time resp))
    (.plus (org.joda.time.Period/parse (str "PT" minutes_in_bed "m")))
    (format-fitbit-datetime )

    ))

(defn- get-sleep-duration [{:keys [minutes_asleep] :as resp}]
  (when minutes_asleep
    (* 60 minutes_asleep)))



(defn- get-sleep-intra [{:keys [minutes_asleep minutes_to_fall_asleep minutes_after_wakeup efficiency] :as resp}]
  (when (and minutes_asleep minutes_to_fall_asleep minutes_after_wakeup efficiency)
    (let [intra_start_time_dt (.plus (parse-fitbit-datetime (get-sleep-start-time resp)) (org.joda.time.Period/parse (str "PT" minutes_to_fall_asleep "m")))
          intra_end_time_dt (.minus (parse-fitbit-datetime (get-sleep-end-time resp)) (org.joda.time.Period/parse (str "PT" minutes_after_wakeup "m")))
          series [(float (/ efficiency 100))]
          period_s (/ (- (.getMillis intra_end_time_dt) (.getMillis intra_start_time_dt)) 1000)
          ]
      {:start_time (format-fitbit-datetime intra_start_time_dt)
       :end_time (format-fitbit-datetime intra_end_time_dt)
       :series series
       :period_s period_s
       :type :sleep
       })))


(defn- sleepresp->sleepevents [resp]

    [(-> resp
       (assoc :start_time (get-sleep-start-time resp))
       (assoc :end_time (get-sleep-end-time resp))
       (assoc :sleep_s (get-sleep-duration resp))
       (assoc :intra (get-sleep-intra resp))
       (assoc :type :sleep)
       (select-keys [:start_time :end_time :type :sleep_s :intra ]))
     ]

  )

(defn get-sleeps
  "get events from [start_date to stop_date)
  start_date and stop_date are RFC3339"
  [consumer access_token start_date stop_date & [params]]
  (let [start_date (format-request-date start_date)
        stop_date (format-request-date stop_date)
         sleep_seriess (pvalues (get-sleep-series-start-time consumer access_token start_date stop_date)
         (get-sleep-series-time-in-bed consumer access_token start_date stop_date)
         (get-sleep-series-minutes-asleep consumer access_token start_date stop_date)
         (get-sleep-series-minutes-to-fall-asleep consumer access_token start_date stop_date)
         (get-sleep-series-minutes-after-wakeup consumer access_token start_date stop_date)

;         (get-sleep-series-minutes-awake consumer access_token start_date stop_date)
         (get-sleep-series-efficiency consumer access_token start_date stop_date))

        ]

  (->> (apply #(map create-sleep-resp %1 %2 %3 %4 %5 %6) sleep_seriess)
  (map sleepresp->sleepevents)
  (flatten)
  identity
    (filter #(and % (< 0 (:sleep_s %))))
  )

  ))
