(ns notch.remotes.util
  "Utility functions"
  (:use clojure.set)
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as http])
  (:require [clojure.string :as str])
  (:require [clojure.java.io :as io])
  (:import [javax.crypto.Mac])
  (:import [javax.crypto.spec.SecretKeySpec])
  (:refer-clojure :exclude (get)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;Administrivia
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-config-file
  "Reads a .clj file from the resources directory."
  [filename]
  (-> (clojure.java.io/resource filename)
    (clojure.java.io/reader)
    (java.io.PushbackReader.)
    (read)))

(defn sort-map
  "Sorts a map by keys"
  [m]
  (into (sorted-map) m))

(defn url-encode
  "Turn keywords, numbers, etc into strings
  Then URLEncode the string"[s]
  (when s
    (-> s
      (#(if (keyword? %) (name %) (str %)))
      (java.net.URLEncoder/encode)
      )))

(defn query-params->map
  "Turn a query param string into a map"
  [param_string]
  (if param_string
    (->> (str/split param_string #"&")
      (map #(str/split % #"="))
      (map (fn [[k v]] [(keyword k) v]))
      (into {}))
    {}))

(defn valid-oauth1-token [t]
 (when (and (:oauth_token t) (:oauth_token_secret t))
   t
   ))

(defn valid-oauth2-token [t]
  (when (:access_token t)
    t
    ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Some date/time helpers
;;;;;;;;;;;;;;;;;

(defn get-datetime-formatter-3339* [& [tz_id]]
  (if (= tz_id "-00:00")
    (-> (org.joda.time.format.DateTimeFormat/forPattern "yyyy-MM-dd'T'HH:mm:ss.SSS-00:00")
      (.withZone (org.joda.time.DateTimeZone/forID "-00:00")))
    (-> (org.joda.time.format.DateTimeFormat/forPattern "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
      (.withZone (org.joda.time.DateTimeZone/forID tz_id)))

    )
  )

(def get-datetime-formatter-3339 (memoize get-datetime-formatter-3339* ))
(def ^:private problem_tz_ids
  {"pdt" "America/Los_Angeles"
   "PDT" "America/Los_Angeles"}
)
(defn timezone-for-id [tz_id]
  (try
    (let [tz_id (get-in problem_tz_ids [tz_id] tz_id)]
      (org.joda.time.DateTimeZone/forID tz_id)
      )
    (catch java.lang.IllegalArgumentException ex
      (org.joda.time.DateTimeZone/forTimeZone (java.util.TimeZone/getTimeZone tz_id)))))


;(timezone-for-id "PDT")
;(timezone-for-id "GMT+0100")
;(timezone-for-id "GMT-0400")
;(timezone-for-id "GMT+0000")
;

(defn get-datetime-period [p_string]
  (org.joda.time.Period/parse p_string))

(defn parse-datetime [dt_string]
  (org.joda.time.DateTime/parse dt_string))

(defn parse-datetime-from-long [dt_long]
  (org.joda.time.DateTime. dt_long))

(defn datetime-with-tz [dt tz_id]
  (.withZone dt (org.joda.time.DateTimeZone/forID tz_id)))

(defn datetime-with-tz-retain-fields [dt tz_id]
  (.withZoneRetainFields dt (org.joda.time.DateTimeZone/forID tz_id)))

(defn datetime->3339-string [dt tz_string]
  (.print (get-datetime-formatter-3339 tz_string) dt))


(defn seconds->datetime [seconds tz_string]
  (-> (parse-datetime-from-long (* 1000 seconds))
    (datetime-with-tz  tz_string)))

(defn datetime->seconds [dt]
  (long (* 0.001 (.getMillis dt))))