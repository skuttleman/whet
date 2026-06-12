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

(defn ^:private with-edn-req [req]
  (let [content-type (or (ring.req/content-type req)
                         "application/edn")
        content-length (or (ring.req/content-length req)
                           0)]
    (-> req
        (cond-> (zero? content-length)
                (dissoc :body)

                (string/starts-with? content-type "application/edn")
                (update :body read-edn)))))

(defn ^:private with-edn-resp [response]
  (cond-> response
    (and (nil? (get-in response [:headers "content-type"]))
         (some? (:body response))
         (not (:whet.core/raw? response)))
    (-> (assoc-in [:headers "content-type"] "application/edn")
        (update :body pr-str))))

(defn ^:private with-route-info [routes {:keys [route uri] :as req}]
  (let [uri (or uri
                (nav/path-for routes
                              (:token route)
                              (:route-params route)
                              (:query-params route)))
        route-info (nav/match routes
                              (cond-> uri
                                (:query-string req) (str "?" (:query-string req))))]
    (assoc req :whet.core/route route-info)))

(defn ^:private with-edn [handler]
  (fn
    ([req]
     (handler (with-edn-req req)))
    ([req respond raise]
     (handler (with-edn-req req) (comp respond with-edn-resp) raise))))

(defn ^:private with-routing [handler routes]
  (fn
    ([req]
     (handler (with-route-info routes req)))
    ([req respond raise]
     (handler (with-route-info routes req) respond raise))))

(defn with-middleware [handler routes]
  (-> handler
      with-edn
      (with-routing routes)))
