(ns notch.remotes.oauth
  "Clojure OAuth client modeled after clj-http"
  (:use clojure.set)
  (:use clojure.tools.logging)
  (:use notch.remotes.util)
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


(defn create-consumer
  "Remap to an OAuth consumer"
  [config]
  (-> (select-keys config [:base_uri
                          :request_token_uri
                          :access_token_uri
                          :authorization_uri
                          :signature_method
                          :oauth_consumer_key
                          :oauth_consumer_secret ])))

(def ^:private secure_random (java.security.SecureRandom.))
(defn- generate-nonce []
  (->
    (java.math.BigInteger. 256 secure_random)
    (.toString 32)))


(defn- assoc-url-param
  "Set a query param in a URL string"
  [uri_string param_name param_string]

  (let [[_ before_params _ params fragment] (re-matches #"^([^?#]*)(\?([^#]*))?(#(.*))?" uri_string)
        params_map (query-params->map params)
        params_map (assoc params_map param_name (url-encode param_string))
        new_param_string (str/join "&" (map #(str (name %1) "=" %2) (keys params_map) (vals params_map)))]
    (str (str/join "?" [before_params new_param_string]) fragment)
    )
  )

(defn- oauth-params->authorization-header
  "Turn a map of OAuth params into the oauth authorization header string"
  [oauth_params]
  (->> oauth_params
    (map (fn [[k v]] (str (url-encode k) "=\"" (url-encode v) "\"" )))
    (str/join ", ")
    (str "OAuth ")
    )
  )

(defn- base64 [bytes]
  (org.apache.commons.codec.binary.Base64/encodeBase64 bytes))

(defn- hmac-sha1
  [signing_key base_string]
  (-> (javax.crypto.spec.SecretKeySpec. (.getBytes signing_key) "HmacSHA1")
    (#(doto (javax.crypto.Mac/getInstance "HmacSHA1") (.init %)))
    (.doFinal (.getBytes base_string))
    (org.apache.commons.codec.binary.Base64/encodeBase64)
    (String. "UTF-8")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;Misc middleware
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn wrap-debug
  "set clj-http debug param to true for all oauth calls"
  [client]
  (fn [req]
    (client (-> req
              (assoc :debug true)
              (assoc :debug-body true)

              ))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;Prepare the OAuth Params
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- generate-oauth-params
  "Create the basic OAuth params"
  [{:keys [consumer token]}]

  (-> {:oauth_consumer_key (:oauth_consumer_key consumer)
       :oauth_signature_method (:signature_method consumer)
       :oauth_timestamp (int (* 0.001 (System/currentTimeMillis)))
       :oauth_nonce (generate-nonce)
       :oauth_version "1.0"}
    (#(if token (assoc % :oauth_token  (:oauth_token token)) %))
    )
  )

(defn- generate-base-string
  "Create the oauth base string"
  [method url params]
  (str (str/upper-case (name method))
       "&" (url-encode url)
       "&" (url-encode (http/generate-query-string (sort-map params) ))))

(defn- sign-base-string
  "Sign the oauth base string. Only does hmac-sha1.. others needed?"
  [consumer base_string & [token]]
  (-> (str (url-encode (:oauth_consumer_secret consumer)) "&" (url-encode (:oauth_token_secret token)))
    (hmac-sha1 base_string)))

(defn- assoc-oauth-params
  "Make sure the oauth params are placed in the right http field"
  [req oauth_param_placement oauth_params]
  (cond

    (= :form-params oauth_param_placement)
    (update-in req [:form-params] #(sort-map (merge % oauth_params)))

    (= :query-params oauth_param_placement)
    (update-in req [:query-params] #(sort-map (merge % oauth_params)))

    ;;Place oauth params in the authorization header by default
    :default
    (assoc-in req [:headers "Authorization"] (oauth-params->authorization-header oauth_params))
    ))


(defn wrap-add-oauth-params
  "Middleware wrapping adding propper oauth params to the clj-http request
  oauth_param_placement should be one of: #{ query-params, form-params, nil}"
  [client]
  (fn [{:keys [method url consumer token query-params form-params oauth_param_placement]
        :as req}]
    ;;Gather up the parameters to be signed
    (let [ ;;Generate the basic oauth params
           oauth_params (generate-oauth-params req)
          ;;Gather up all parameters that need to be signed
          params_to_sign (into (sorted-map) (merge oauth_params query-params form-params))
          ;;Generate the base string from these parameters
          base_string (generate-base-string method url params_to_sign)
          ;;Sign the base string
          oauth_signature (sign-base-string consumer base_string token)
          ;;Insert the signature at the begining of the oauth params map
          oauth_params (into (array-map :oauth_signature oauth_signature) oauth_params )
          ]
      (client (-> req
                (assoc-oauth-params oauth_param_placement oauth_params )
                (dissoc :consumer :token :oauth_param_placement)
                )))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;The clj-http request function, wrapped with oauth middleware
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private request
  (-> http/request
;    (wrap-debug)
    (wrap-add-oauth-params)

    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;OAuth 1.0 Authentication Helper functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-request-token
  "Get request token for OAuth1.0 start"
  ([consumer & [callback_url]]
    (-> {:method :post
         :url (:request_token_uri consumer)
         :consumer consumer
         :oauth_param_placement :form-params}
      ;;If callback_url is included, add it to the form params
      (#(if callback_url (assoc-in % [:form-params :oauth_callback] callback_url) %))
      (request)
      (:body)
      (query-params->map)

      )))

(defn get-authorization-uri
  "Send the user to this URL for 2nd part of OAuth"
  [consumer request_token & [callback_url]]
  (-> (:authorization_uri consumer)
    (assoc-url-param :oauth_token (:oauth_token request_token))
    (#(if callback_url (assoc-url-param % :oauth_callback callback_url) %))
    ))



(defn get-access-token
  "Get the final access token for OAuth1.
  Returns the token as a map if successful"
  [consumer request_token & [oauth_verifier]]
  (-> {:method :post
       :url (:access_token_uri consumer)
       :consumer consumer
       :token request_token
       :oauth_param_placement :form-params}
    (#(if oauth_verifier (assoc-in % [:form-params :oauth_verifier ] oauth_verifier) %))
    (request)
    (:body)
    (query-params->map)
    ))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;HTTP Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn get
  "Helper function for after the Authentication process"
  [url & [{:keys [consumer token]
           :as req}]]
  (-> { :method :get
        :url url
        :consumer consumer
        :token token}
    (merge req)
    (request)
    :body
))
