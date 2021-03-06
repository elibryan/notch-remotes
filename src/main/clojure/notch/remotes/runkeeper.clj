(ns notch.remotes.runkeeper
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
    )

  )

(defn get-user [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/user")
    )
  )

(defn get-user-profile [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token (:profile (get-user oauth_consumer access_token )))
    )
  )

(defn get-user-settings [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token (:settings (get-user oauth_consumer access_token )))
    )
  )

(defn get-fitness-activities
  "Returns a list of fitness activities (runs)"
  [oauth_consumer access_token & [page page_size]]
  (let [uri (:fitness_activities (get-user oauth_consumer access_token ))]
    (-> (http-get oauth_consumer access_token uri {:page (or page 0)
                                               :pageSize (or page_size 20)})
    :items)
      ))



(defn get-fitness-activity
  "returns the details of a fitness activity"
  [oauth_consumer access_token activity_uri]
  (-> (http-get oauth_consumer access_token activity_uri)))