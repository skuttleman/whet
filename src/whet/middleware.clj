(ns whet.middleware
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]
    [ring.util.request :as ring.req]))

(defn ^:private with-edn [handler]
  (fn [req]
    (let [content-type (or (ring.req/content-type req)
                           "application/edn")
          response (-> req
                       (cond-> (string/starts-with? content-type "application/edn")
                               (update :body edn/read))
                       handler)]
      (cond-> response
        (and (nil? (get-in response [:headers "content-type"]))
             (some? (:body response)))
        (-> (assoc-in [:headers "content-type"] "application/edn")
            (update :body pr-str))))))

(defn ^:private with-routing [handler routes]
  (fn [req]
    #_
    (let [route-info (rte/match (cond-> (:uri req)
                                  (:query-string req) (str "?" (:query-string req))))]
      (handler (assoc req :brainard/route route-info)))))

(defn with-base [handler routes]
  (-> handler
      with-edn
      (with-routing routes)))

(defn with-hydration
  ([handler ui-handler]
   (with-hydration handler ui-handler nil))
  ([handler ui-handler opts]
   (fn [req])))
