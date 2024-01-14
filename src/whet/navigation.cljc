(ns whet.navigation
  (:require
    #?(:cljs [pushy.core :as pushy])
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [defacto.core :as defacto]
    [whet.interfaces :as iwhet])
  #?(:clj
     (:import
       (java.net URLEncoder URLDecoder))))

(defn ^:private encode [s]
  #?(:cljs    (js/encodeURIComponent s)
     :default (URLEncoder/encode ^String s "UTF8")))

(defn ^:private decode [s]
  #?(:cljs    (js/decodeURIComponent s)
     :default (URLDecoder/decode ^String s "UTF8")))

(defn ^:private kw->str [kw]
  (if-not (keyword? kw)
    kw
    (let [ns (namespace kw)]
      (cond->> (name kw)
        ns (str ns "/")))))

(defn ->query-string
  "Converts a map of query-params into a query-string.

  (->query-string {:a 1 :b [:foo :bar]})
  ;; => \"a=1&b=foo&b=bar\""
  [params]
  (some->> params
           (mapcat (fn [[k v]]
                     (when (some? v)
                       (map (fn [v']
                              (cond-> (name k)
                                (not (true? v'))
                                (str "=" (encode (str (kw->str v'))))))
                            (cond-> v (not (coll? v)) vector)))))
           seq
           (string/join "&")))

(defn ^:private wrap-set [x]
  (cond->> x
    (not (set? x)) (conj #{})))

(defn ->query-params
  "Parses a query-string into a map of params. Param values will be a string or set of strings.

  (->query-params \"a=1&b=foo&b=bar\")
  ;; => {:a \"1\" :b #{\"foo\" \"bar\"}}"
  [query]
  (when (seq query)
    (reduce (fn [params pair]
              (let [[k v] (string/split pair #"=")
                    k (keyword k)
                    v (if (re-find #"=" pair) (decode (str v)) true)]
                (if (contains? params k)
                  (assoc params k (conj (wrap-set (get params k)) v))
                  (assoc params k v))))
            {}
            (string/split query #"&"))))

(defn ^:private with-qp [uri query-params]
  (let [query (->query-string query-params)]
    (cond-> uri
      query (str "?" query))))

(defn path-for
  "Produces a path from a route handle and optional params."
  ([routes token]
   (path-for routes token nil))
  ([routes token route-params]
   (path-for routes token route-params nil))
  ([routes token route-params query-params]
   (let [route-info (apply bidi/path-for routes token (flatten (seq route-params)))]
     (with-qp route-info query-params))))

(defn match
  "Matches a route uri and parses route info."
  [routes uri]
  (let [[path query-string] (string/split uri #"\?")
        anchor #?(:cljs (some-> js/document.location.hash not-empty (subs 1))
                  :default nil)
        {:keys [handler route-params]} (bidi/match-route routes path)]
    (merge {:token        handler
            :uri          uri
            :route-params (iwhet/coerce-route-params handler route-params)
            :query-params (->query-params query-string)
            :anchor       anchor})))

(defn navigate!
  "Takes a routing token and optional params and pushes it to the browser's history"
  ([nav token]
   (navigate! nav token nil))
  ([nav token route-params]
   (navigate! nav token route-params nil))
  ([nav token route-params query-params]
   (iwhet/navigate! nav token route-params query-params)))

(defn replace!
  "Takes a routing token and optional params and replaces the current route"
  ([nav token]
   (replace! nav token nil))
  ([nav token route-params]
   (replace! nav token route-params nil))
  ([nav token route-params query-params]
   (iwhet/replace! nav token route-params query-params)))

#?(:cljs
   (deftype PushyNavigator [routes ^:volatile-mutable -pushy]
     iwhet/INavigate
     (navigate! [_ token route-params query-params]
       (pushy/set-token! -pushy (path-for routes token route-params query-params)))
     (replace! [_ token route-params query-params]
       (pushy/replace-token! -pushy (path-for routes token route-params query-params)))

     defacto/IInitialize
     (init! [_ store]
       (let [pushy (pushy/pushy #(defacto/emit! store [:whet.core/navigated %])
                                (partial match routes))]
         (set! -pushy pushy)
         (pushy/start! pushy)))))
