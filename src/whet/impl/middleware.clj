(ns whet.impl.middleware
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [ring.util.request :as ring.req]
    [whet.utils.navigation :as nav])
  (:import
    (java.io InputStream PushbackReader Reader)))

(defn ^:private read-edn [is]
  (if-not (or (instance? InputStream is)
              (instance? Reader is))
    is
    (let [reader (-> is
                     io/reader
                     PushbackReader.)
          byte (.read reader)]
      (when-not (= -1 byte)
        (.unread reader byte)
        (edn/read reader)))))

(defn ^:private with-edn [handler]
  (fn [req]
    (let [content-type (or (ring.req/content-type req)
                           "application/edn")
          response (-> req
                       (cond-> (string/starts-with? content-type "application/edn")
                               (update :body read-edn))
                       handler)]
      (cond-> response
        (and (nil? (get-in response [:headers "content-type"]))
             (some? (:body response)))
        (-> (assoc-in [:headers "content-type"] "application/edn")
            (update :body pr-str))))))

(defn ^:private with-routing [handler routes]
  (fn [{:keys [route uri] :as req}]
    (let [uri (or uri
                  (nav/path-for routes
                                (:token route)
                                (:route-params route)
                                (:query-params route)))
          route-info (nav/match routes
                                (cond-> uri
                                  (:query-string req) (str "?" (:query-string req))))]
      (handler (assoc req :whet.core/route route-info)))))

(defn with-middleware [handler routes]
  (-> handler
      with-edn
      (with-routing routes)))
