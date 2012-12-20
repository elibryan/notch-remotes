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
  (:require [notch.remotes.oauth :as oauth1] :reload)
  (:require [notch.remotes.oauth2 :as oauth2] :reload)
  (:require [notch.remotes.fitbit :as fitbit] :reload)
  (:require [notch.remotes.mapmyfitness :as mapmyfitness] :reload)
  (:require [notch.remotes.bodymedia :as bodymedia] :reload)
  (:require [notch.remotes.withings :as withings] :reload)
  (:require [notch.remotes.runkeeper :as runkeeper] :reload)
  (:require [notch.remotes.facebook :as facebook] :reload)
  (:require [notch.remotes.google :as google] :reload)
  )





(def consumer (oauth1/create-consumer (:mapmyfitness (load-config-file "remotes.config.clj"))))
(def consumer (oauth1/create-consumer (:bodymedia (load-config-file "remotes.config.clj"))))
(def consumer (oauth1/create-consumer (:fitbit (load-config-file "remotes.config.clj"))))
(def consumer (oauth1/create-consumer (:withings (load-config-file "remotes.config.clj"))))




(try ;;Get a request token
  (def request_token
    (oauth1/get-request-token consumer "http://localhost")
    )
  request_token
  (catch Exception e (error e)))


;;Go to the site
(clojure.java.browse/browse-url (oauth1/get-authorization-uri consumer request_token "http://localhost"))


(try ;;Get an access token to the site
  (def access_token
;    (oauth/get-access-token consumer request_token)
    (oauth/get-access-token consumer request_token "j5qpp91m6cohot8ojk3learldi")
    )
  access_token
  (catch Exception e (error e)))



(fitbit/get-user consumer access_token )
(fitbit/get-steps-series consumer access_token "2012-01-01" "2012-01-31")
(fitbit/get-calories-series consumer access_token "2012-01-01" "2012-01-31")
(fitbit/get-weight-series consumer access_token "2012-01-01" "2012-01-31")
(fitbit/get-calories-in-series consumer access_token "2012-01-01" "2012-01-31")
(fitbit/get-water-in-series consumer access_token "2012-01-01" "2012-01-31")
(fitbit/get-sleep-series-start-time consumer access_token "2012-01-01" "2012-01-31")
(fitbit/get-sleep-series-time-in-bed consumer access_token "2012-01-01" "2012-01-31")
(fitbit/get-sleep-series-minutes-asleep consumer access_token "2012-01-01" "2012-01-31")
(bodymedia/get-user consumer access_token )
(bodymedia/get-user-last-sync consumer access_token)
(bodymedia/get-weight-measurements consumer access_token)
(bodymedia/get-burn-days consumer access_token "20121001" "20121101")
(bodymedia/get-step-days consumer access_token "20121001" "20121101")
(bodymedia/get-sleep-days consumer access_token "20121001" "20121101")
(mapmyfitness/get-user consumer access_token )
(mapmyfitness/get-workouts consumer access_token )
(count (mapmyfitness/get-workouts consumer access_token 0 3))
(is (= (mapmyfitness/get-workouts consumer access_token 0 3)
      (vec (concat (mapmyfitness/get-workouts consumer access_token 0 1)
             (mapmyfitness/get-workouts consumer access_token 1 1)
             (mapmyfitness/get-workouts consumer access_token 2 1)))))
(mapmyfitness/get-workout-full consumer access_token (:workout_id (first (mapmyfitness/get-workouts consumer access_token ))))
(mapmyfitness/get-route consumer access_token (:route_id (first (mapmyfitness/get-workouts consumer access_token ))))
(mapmyfitness/get-workout-time-series consumer access_token (:workout_id (first (mapmyfitness/get-workouts consumer access_token ))))
(withings/get-user consumer access_token )





(def consumer (oauth2/create-consumer (:google (load-config-file "remotes.config.clj"))))
(def consumer (oauth2/create-consumer (:facebook (load-config-file "remotes.config.clj"))))
(def consumer (oauth2/create-consumer (:runkeeper (load-config-file "remotes.config.clj"))))



(->
  (oauth2/get-authorization-uri consumer
    { :redirect_uri "http://notch.me/"
      :state "somestatehere"
;      :scope "https://www.googleapis.com/auth/latitude.all.best https://www.googleapis.com/auth/plus.me"
      :scope "publish_stream,read_stream,export_stream"
      } )
  (clojure.java.browse/browse-url)
  )



(try ;;Get an access token to the site
  (def access_token
    (oauth2/get-access-token consumer
      ""
      "http://notch.me/")

    )
  access_token
  (catch Exception e (error e)))

(google/get-user consumer access_token)
(facebook/get-user consumer access_token)
(facebook/get-feed consumer access_token 0 400)
(facebook/get-feed-by-time consumer access_token 1325404800  1356940800 1000)
(runkeeper/get-user-profile consumer access_token)
(runkeeper/get-user consumer access_token)
(runkeeper/get-user-settings consumer access_token)
(is (= (runkeeper/get-fitness-activities consumer access_token 0 3)
       (vec (concat (runkeeper/get-fitness-activities consumer access_token 0 1)
                    (runkeeper/get-fitness-activities consumer access_token 1 1)
                    (runkeeper/get-fitness-activities consumer access_token 2 1)))))
(runkeeper/get-fitness-activity consumer access_token (:uri (first (runkeeper/get-fitness-activities consumer access_token 0 1))))

;(run-tests 'notch.remotes.test-basics)
