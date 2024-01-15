(ns whet.impl.http
  (:require
    [cljs-http.client :as http]
    [clojure.core.async :as async]
    [defacto.resources.core :as-alias res]
    [whet.interfaces :as iwhet]
    [whet.utils.navigation :as nav]))

(defn ^:private success? [status]
  (and (integer? status)
       (<= 200 status 299)))

(defn ^:private prep [routes params]
  (let [{:keys [token route-params query-params]} (:route params)
        url (nav/path-for routes token route-params query-params)]
    (-> params
        (dissoc :route)
        (assoc :url url)
        (assoc-in [:headers "content-type"] "application/edn")
        (update :body #(some-> % pr-str)))))

(defmethod iwhet/handle-request :default
  [_ routes params]
  (async/go
    (let [params (prep routes params)
          {:keys [status body]} (async/<! (http/request params))]
      (if (success? status)
        [::res/ok body]
        [::res/err body]))))
