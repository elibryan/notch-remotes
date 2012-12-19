(ns notch.remotes.facebook
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
    (-> (http/get (str (:base_uri consumer) path) {:debug false :query-params query_params})
      (:body)
      (json/read-str :key-fn keyword)
      )))

(defn get-user [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/me")
    )
  )

(defn get-feed [oauth_consumer access_token & [page page_size] ]
  (-> (http-get oauth_consumer access_token "/me/feed" {:offset (* (or page 0) page_size)
                                                        :limit (or page_size 50)})
   (:data)
    )
  )

(defn get-feed-by-time [oauth_consumer access_token & [start_date stop_date limit] ]
  (-> (http-get oauth_consumer access_token "/me/feed" {:since start_date
                                                        :until stop_date
                                                        :limit limit})
    (:data)
    )
  )