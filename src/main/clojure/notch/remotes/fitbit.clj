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