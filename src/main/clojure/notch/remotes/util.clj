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