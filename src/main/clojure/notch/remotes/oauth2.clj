(ns notch.remotes.oauth2
  "Clojure OAuth2 helper functions"
  (:use clojure.set)
  (:use clojure.tools.logging)
  (:use notch.remotes.util :reload)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as http])
  (:require [clojure.string :as str])
  (:require [clojure.java.io :as io]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-consumer
  "Remap to an OAuth consumer"
  [config]
  (-> (select-keys config [:base_uri
                           :access_token_uri
                           :authorization_uri
                           :client_id
                           :client_secret ])))


(defn get-authorization-uri
  "Send the user to this URL for first part of OAuth2"
  [consumer & [opts]]

  (let [query_string (-> {:client_id (:client_id consumer)
                          :response_type "code"}
                          (merge opts)
                          ;(merge (select-keys opts [:redirect_uri :scope :state] ))
                        (http/generate-query-string))
        ]
      (str (:authorization_uri consumer) "?" query_string)
      ))



(defn- coerce-response-body
  "Attempt to turn the response into a clojure map"
  [response]
  (let [content_type (get-in response [:headers "content-type"])
;       _ (println content_type)
        body_string (:body response)]
    (cond
      (re-matches #"^application/json.*" content_type)
      (json/read-str body_string :key-fn keyword)

      (re-matches #"^text/plain.*" content_type)
      (query-params->map body_string)

      :default
      body_string
      )
 ))


(defn get-access-token
  "Complete the OAuth 2 process"
  [consumer code redirect_uri  ]
  (let [form_params { :grant_type "authorization_code"
                       :client_id (:client_id consumer)
                      :redirect_uri redirect_uri
                      :client_secret (:client_secret consumer)
                      :code code}
       response (http/post (:access_token_uri consumer) {:form-params form_params})
;       _ (debug response)
    ]
    (valid-oauth2-token (coerce-response-body response))
))
