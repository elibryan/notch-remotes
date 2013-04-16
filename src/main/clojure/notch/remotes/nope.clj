(ns notch.remotes.nope
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as str])
  (:require [notch.remotes.oauth2 :as oauth] :reload)
  (:require [clj-http.client :as http])
  )

(defn http-get
  "Helper function for OAuth HTTP Gets"
  [consumer access_token path & [query_params]]
  (let [query_params query_params]
    (-> (http/get (str (:base_uri consumer) path) {:query-params query_params
;                                                   :debug true
                                                   :oauth-token (:access_token access_token)
                                                   ;;correct for service returning a set-cookie header that httpclient doesn't like
                                                   :response-interceptor (fn [resp ctx] (.setHeader resp "Set-Cookie" ""))})
      (:body)
      (json/read-str :key-fn keyword)
      )))


(do "Misc Helper stuff"

;;;;;;;;;;;;;;;;;;;;;;;;
;; Date formatting
;;;;;;;;;;;;;;;;
^:private
(def date_format (doto (java.text.SimpleDateFormat. "yyyyMMdd")
                   (.setTimeZone (java.util.TimeZone/getTimeZone "GMT-0:00" ))))
(defn- format-date [date]
  (.format date_format date))

(defn parse-date [date_string]
  (when (not (re-matches #"^\d\d\d\d\d\d\d\d$" date_string)) (throw (IllegalArgumentException. (str "Invalid date syntax: " date_string))))
  (.parse date_format date_string))

^:private
(def milliseconds_per_day (* 24 60 60 1000))
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Basic getter calls
;;;;;;;;;;;;;;;;;

(defn get-user
  "Get user's profile data"
  [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/nudge/api/users/@me")))

(defn get-friends [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/nudge/api/users/@me/friends")))

(defn get-mood [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/nudge/api/users/@me/mood")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event getters (steps, sleep, weight, etc)
;;;;;;;;;;;;;;;;;
(defn- get-date-query-param-sets
  "get formatted date query params, from [start_date to stop_date).. "
  [start_date stop_date]

  (let [start_date (parse-date start_date)
        stop_date (parse-date stop_date)]
  (for [cur_date (range (.getTime start_date) (.getTime stop_date) milliseconds_per_day)]
    {:date (format-date (java.util.Date. cur_date))})))

(defn- get-stuff
  "generic function for downloading events"
  [oauth_consumer access_token start_date stop_date get-day-fn get-intra-day-fn & [{intra_day :intra_day}]]
  (->>
    ;;Generate queries for date range
    (get-date-query-param-sets start_date stop_date)
    ;;Download the day-level dates
    (pmap #(get-day-fn oauth_consumer access_token (:date %)))
    (flatten)
    (filter identity) ;;remove nils
    ;;if intra_day flag, then download the intra-day leve
    (#(if (and intra_day get-intra-day-fn) (pmap (fn [o] (assoc o :intra (get-intra-day-fn oauth_consumer access_token (:xid o) ))) %) %))
    )
  )

(defn get-moves-day
  "Get moves for the day on date_string"
  [oauth_consumer access_token date_string & [options]]
  (->> (http-get oauth_consumer access_token "/nudge/api/users/@me/moves" {:date date_string})
    :data :items))

(defn get-move-intensity [oauth_consumer access_token xid]
  (->> (http-get oauth_consumer access_token (str "/nudge/api/moves/" xid "/snapshot"))
    :data))

(defn get-moves
  "get events from [start_date to stop_date)
  start_date and stop_date are YYYYMMDD format"
  [oauth_consumer access_token start_date stop_date & [options]]
  (get-stuff oauth_consumer access_token start_date stop_date get-moves-day get-move-intensity options))


(defn get-sleeps-day
  "get a single sleep on date_string"
  [oauth_consumer access_token date_string & [options]]
  (->> (http-get oauth_consumer access_token "/nudge/api/users/@me/sleeps" {:date date_string})
    :data :items))

(defn get-sleep-intensity [oauth_consumer access_token xid]
  (->> (http-get oauth_consumer access_token (str "/nudge/api/sleeps/" xid "/snapshot"))
    :data))

(defn get-sleeps
  "get events from [start_date to stop_date)
  start_date and stop_date can be java.util.Date or RFC3339 strings"
  [oauth_consumer access_token start_date stop_date & [options]]
  (get-stuff oauth_consumer access_token start_date stop_date get-sleeps-day get-sleep-intensity options))


(defn get-body-events-day
  "get body events on date_string"
  [oauth_consumer access_token date_string & [options]]
  (->> (http-get oauth_consumer access_token "/nudge/api/users/@me/body_events" {:date date_string})
    :data :items))

(defn get-body-events
  "get events from [start_date to stop_date)
  start_date and stop_date can be java.util.Date or RFC3339 strings"
  [oauth_consumer access_token start_date stop_date & [options]]
  (get-stuff oauth_consumer access_token start_date stop_date get-body-events-day nil options))
