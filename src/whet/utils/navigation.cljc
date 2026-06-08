(ns whet.utils.navigation
  (:require
    [clojure.string :as string]
    [reitit.coercion :as coercion]
    [reitit.core :as r]
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

(defn ^:private wrap-set [x]
  (cond->> x
    (not (set? x)) (conj #{})))

(defn ->query-string
  "Converts a map of query-params into a query-string.

  (->query-string {:a 1 :b [:foo :bar]})
  ;; => \"a=1&b=foo&b=bar\""
  [params]
  (some->> params
           (mapcat (fn [[k v]]
                     (when (some? v)
                       (map (fn [v']
                              (str (name k) "=" (encode (str (kw->str v')))))
                            (cond-> v (not (coll? v)) vector)))))
           seq
           (string/join "&")))

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

(defn ^:private route-param-keys
  "Returns a map from reitit path-param keyword to app-domain namespaced keyword for a named route."
  [router token]
  (get-in (r/match-by-name router token) [:data :param-keys] {}))

(defn path-for
  "Produces a path from a route handle and optional params."
  ([router token]
   (path-for router token nil))
  ([router token route-params]
   (path-for router token route-params nil))
  ([router token route-params query-params]
   (let [param-keys (route-param-keys router token)
         inv-keys (into {} (map (fn [[k v]] [v k])) param-keys)
         reitit-params (into {} (map (fn [[k v]] [(get inv-keys k k) (str (kw->str v))])) route-params)
         path (:path (r/match-by-name router token reitit-params))]
     (with-qp path query-params))))

(defn match
  "Matches a route uri and parses route info."
  [router uri]
  (let [[path query-string] (string/split uri #"\?")
        anchor #?(:cljs (some-> js/document.location.hash not-empty (subs 1))
                  :default nil)
        m (r/match-by-path router path)
        token (get-in m [:data :name])
        param-keys (get-in m [:data :param-keys] {})
        coerced-path (:path (coercion/coerce! m))
        route-params (into {}
                           (map (fn [[k v]]
                                  [(get param-keys k k) v]))
                           (or coerced-path (:path-params m)))]
    (cond-> {:token        token
             :uri          uri
             :route-params route-params
             :query-params (->query-params query-string)}
      anchor (assoc :anchor anchor))))

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
