(ns notch.remotes.jawbone
  (:use clojure.tools.logging)
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as str])
  (:require [notch.remotes.oauth2 :as oauth] :reload)
  (:require [clj-http.client :as http])
  (:use notch.remotes.util :reload)
  )

(defn- service-req [req consumer access_token]
  (-> {:url (str (:base_uri consumer) (:path req))
       ;   :debug true
       :oauth-token (:access_token access_token)
       ;;correct for service returning a set-cookie header that httpclient doesn't like
       :response-interceptor (fn [resp ctx] (.setHeader resp "Set-Cookie" ""))
       }
    (merge req)))

(defn request
  "create authed request"
  [consumer access_token req]
  (->
    (service-req req consumer access_token)
    (http/request)
    (:body)
    (json/read-str :key-fn keyword)
    )
  )

(defn get-moves-request
  ""
  [{:keys [start_time end_time] :as req}]
  (-> {:method :get
       :path "/nudge/api/users/@me/moves"
       :query-params { :start_time (datetime->seconds (parse-datetime start_time))
                     :end_time (datetime->seconds (parse-datetime end_time))}}))

(defn get-move-intensity-request
  ""
  [{:keys [xid] :as req}]
  (-> {:method :get
       :path (str "/nudge/api/moves/" xid "/snapshot")}))


(defn resp-item->start-time [{time_created :time_created
                              {tz :tz} :details}]
  (-> (seconds->datetime time_created tz)
     (datetime->3339-string tz)))

(defn resp-item->end-time [{time_completed :time_completed
                            {tz :tz} :details}]
  (-> (seconds->datetime time_completed tz)
    (datetime->3339-string tz)))

(defn- resp-item->intraday-series [{{tz :tz} :details
                                     intra :intra
                                     :as resp} type normalize-value]
  (when (< 1 (count intra))
    (let [intra (sort-map (map #(hash-map (first %) (second %)) intra))
          timestamps (vec (map key intra))
          start_time_s (first timestamps)
          end_time_s (last timestamps)
          start_time (datetime->3339-string (seconds->datetime start_time_s tz) tz)
          end_time (datetime->3339-string (seconds->datetime end_time_s tz) tz)
          period_s (- (second timestamps) (first timestamps))
          series (map #(normalize-value (val %)) intra)]
      {:start_time start_time :end_time end_time :period_s period_s :type type :series series}
      )))

(defn resp-item->step-event [resp_item]
;  (println resp_item)
  [(-> resp_item
     (assoc :start_time (resp-item->start-time resp_item))
     (assoc :end_time (resp-item->end-time resp_item))
     (assoc :steps (get-in resp_item [:details :steps]))
     (assoc :intra (resp-item->intraday-series resp_item :steps long))
     (assoc :type :steps)
     (select-keys [:start_time :end_time :type :steps :intra ])
     (#(when (every? % [:start_time :end_time :type :steps]) %)))
   ])

(defn resp-item->burn-event [resp_item]
  [(-> resp_item
     (assoc :start_time (resp-item->start-time resp_item))
     (assoc :end_time (resp-item->end-time resp_item))
     (assoc :calories (get-in resp_item [:details :calories]))
     (assoc :type :burn)
     (select-keys [:start_time :end_time :type :calories ])
     (#(when (every? % [:start_time :end_time :type :calories]) %)))
   ])

(defn get-moves
  [oauth_consumer access_token {:keys [start_time end_time intra_day]}]
  (let [ resp_items (-> (request oauth_consumer access_token (get-moves-request {:start_time start_time :end_time end_time })) :data :items)
         intra_items (when intra_day (->> (map :xid resp_items)
                                          (pmap #(request oauth_consumer access_token (get-move-intensity-request {:xid %}) ) )))
          resp_items (if (not-empty intra_items) (map #(assoc %1 :intra (:data %2)) resp_items intra_items) resp_items)
         output (->>
                    (map resp-item->step-event resp_items)
                    (concat (map resp-item->burn-event resp_items))
                    (flatten)
                    (filter identity))]
  output
  ))