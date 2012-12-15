(ns notch.remotes.test-basics
  (:use clojure.set)
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as http])
  (:require [clojure.java.io :as io])
  (:require [clojure.java.browse])
  (:use clojure.test)
  (:require [clojure.string :as str])
  (:require [notch.remotes.oauth :as oauth] :reload)
  (:require [notch.remotes.fitbit :as fitbit] :reload)
  (:require [notch.remotes.mapmyfitness :as mapmyfitness] :reload)
  (:require [notch.remotes.bodymedia :as bodymedia] :reload)
  (:require [notch.remotes.withings :as withings] :reload)
  )


(defn load-config-file
  "Reads a .clj file from the resources directory."
  [filename]
  (-> (clojure.java.io/resource filename)
    (clojure.java.io/reader)
    (java.io.PushbackReader.)
    (read)))


(def mapmyfitness_consumer (oauth/create-oauth1-consumer (:mapmyfitness (load-config-file "remotes.config.clj"))))
(def bodymedia_consumer (oauth/create-oauth1-consumer (:bodymedia (load-config-file "remotes.config.clj"))))
(def fitbit_consumer (oauth/create-oauth1-consumer (:fitbit (load-config-file "remotes.config.clj"))))
(def withings_consumer (oauth/create-oauth1-consumer (:withings (load-config-file "remotes.config.clj"))))
(def consumer withings_consumer )




(try ;;Get a request token
  (def request_token
    (oauth/get-request-token consumer "http://localhost")
    )
  request_token
  (catch Exception e (error e)))


;;Go to the site
(clojure.java.browse/browse-url (oauth/get-authorization-uri consumer request_token "http://localhost"))


(try ;;Get an access token to the site
  (def access_token
    (oauth/get-access-token consumer request_token)
;    (oauth/get-access-token consumer request_token "85iomsvant6s8801dral156gos")
    )
  access_token
  (catch Exception e (error e)))


(fitbit/get-user consumer access_token )
(mapmyfitness/get-user consumer access_token )
(bodymedia/get-user consumer access_token )
(withings/get-user consumer access_token )



;(run-tests 'notch.remotes.test-basics)
