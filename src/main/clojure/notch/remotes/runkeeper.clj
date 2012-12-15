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