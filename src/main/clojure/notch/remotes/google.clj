(ns notch.remotes.google
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as str])
  (:require [notch.remotes.oauth2 :as oauth] :reload)
  (:require [clj-http.client :as http])
  )

(defn- http-get
  "Helper function for OAuth HTTP Gets"
  [consumer access_token path & [query_params]]

  (let [query_params (merge query_params {:access_token (:access_token access_token)})]
    (-> (http/get (str (:base_uri consumer) path) {:query-params query_params})
      (:body)
      (json/read-str :key-fn keyword)
      )
    ))


(defn get-user [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "https://www.googleapis.com/plus/v1/people/me")
    )
  )

(defn get-latitude-history [oauth_consumer access_token & [opts]]
  (-> (http-get oauth_consumer access_token
        "https://www.googleapis.com/latitude/v1/location"
        (select-keys opts [:granularity :min-time :max-time :max-results]) )
  :data
  :items
  ))
