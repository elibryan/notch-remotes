(ns notch.remotes.test-basics
  (:use clojure.set)
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as http])
  (:require [clojure.java.io :as io])
  (:require [clojure.java.browse])
  (:use clojure.test)
  (:require [clojure.string :as str])
  (:use notch.remotes.util)
  (:require [notch.remotes.oauth :as oauth] :reload)
  (:require [notch.remotes.oauth2 :as oauth2] :reload)
  (:require [notch.remotes.fitbit :as fitbit] :reload)
  (:require [notch.remotes.mapmyfitness :as mapmyfitness] :reload)
  (:require [notch.remotes.bodymedia :as bodymedia] :reload)
  (:require [notch.remotes.withings :as withings] :reload)
  (:require [notch.remotes.runkeeper :as runkeeper] :reload)
  (:require [notch.remotes.facebook :as facebook] :reload)
  (:require [notch.remotes.google :as google] :reload)
  )





(def mapmyfitness_consumer (oauth/create-oauth1-consumer (:mapmyfitness (load-config-file "remotes.config.clj"))))
(def bodymedia_consumer (oauth/create-oauth1-consumer (:bodymedia (load-config-file "remotes.config.clj"))))
(def fitbit_consumer (oauth/create-oauth1-consumer (:fitbit (load-config-file "remotes.config.clj"))))
(def withings_consumer (oauth/create-oauth1-consumer (:withings (load-config-file "remotes.config.clj"))))
(def consumer withings_consumer )




;(try ;;Get a request token
;  (def request_token
;    (oauth/get-request-token consumer "http://localhost")
;    )
;  request_token
;  (catch Exception e (error e)))
;
;
;;;Go to the site
;(clojure.java.browse/browse-url (oauth/get-authorization-uri consumer request_token "http://localhost"))
;
;
;(try ;;Get an access token to the site
;  (def access_token
;    (oauth/get-access-token consumer request_token)
;;    (oauth/get-access-token consumer request_token "85iomsvant6s8801dral156gos")
;    )
;  access_token
;  (catch Exception e (error e)))



;(fitbit/get-user consumer access_token )
;(mapmyfitness/get-user consumer access_token )
;(bodymedia/get-user consumer access_token )
;(withings/get-user consumer access_token )





(def consumer (oauth2/create-consumer (:google (load-config-file "remotes.config.clj"))))
(def consumer (oauth2/create-consumer (:facebook (load-config-file "remotes.config.clj"))))
(def consumer (oauth2/create-consumer (:runkeeper (load-config-file "remotes.config.clj"))))



(->
  (oauth2/get-authorization-uri consumer
    { :redirect_uri "http://notch.me/"
      :state "somestatehere"
;      :scope "https://www.googleapis.com/auth/latitude.all.best https://www.googleapis.com/auth/plus.me"
;      :scope "publish_stream,user_location"
      } )
  (clojure.java.browse/browse-url)
  )



(try ;;Get an access token to the site
  (def access_token
    (oauth2/get-access-token consumer
      "code"
      "http://notch.me/")

    )
  access_token
  (catch Exception e (error e)))




(google/get-user consumer access_token)
(facebook/get-user consumer access_token)
(runkeeper/get-user-profile consumer access_token)
(runkeeper/get-user consumer access_token)


;(run-tests 'notch.remotes.test-basics)
