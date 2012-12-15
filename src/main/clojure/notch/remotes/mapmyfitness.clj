(ns notch.remotes.mapmyfitness
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
                                                        :query-params query_params
                                                        :oauth_param_placement :query-params})
      (json/read-str :key-fn keyword))

  )


(defn get-user [oauth_consumer access_token]
  (-> (http-get oauth_consumer access_token "/users/get_user")
    :result
    :output
    :user
    )
  )


(defn get-workouts
  "Returns a list of workouts (runs)"
  [oauth_consumer access_token & [page_number page_size]]
    (->> (http-get oauth_consumer access_token "/workouts/get_workouts" {:start_record (* (or page_number 0) (or page_size 25))
                                                          :limit (or page_size 25)})
      :result
      :output
      :workouts))

(defn get-workout-full
  "Get details of single workout"
  [oauth_consumer access_token workout_id]
  (->> (http-get oauth_consumer access_token "/workouts/get_workout_full" {:workout_id workout_id})
    :result
    :output
    :workout
    ))

(defn get-route
  "Get a route"
  [oauth_consumer access_token route_id]
  (->> (http-get oauth_consumer access_token "/routes/get_route" {:route_id route_id})
    :result
    :output

    ))

(defn get-workout-time-series
  "Get a workout time series"
  [oauth_consumer access_token workout_id]
  (->> (http-get oauth_consumer access_token "/workouts/get_time_series" {:workout_id workout_id})
    :workout
    ))