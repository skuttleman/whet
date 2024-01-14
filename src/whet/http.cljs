(ns whet.http
  (:require
    [clojure.core.async :as async]
    [cljs-http.client :as http]
    [defacto.resources.core :as-alias res]))

(defn ^:private success? [status]
  (and (integer? status)
       (<= 200 status 299)))

(defn request-fn [_ params]
  (async/go
    (let [{:keys [status body]} (async/<! (http/request params))]
      (if (success? status)
        [::res/ok (:data body)]
        [::res/err (:errors body)]))))
