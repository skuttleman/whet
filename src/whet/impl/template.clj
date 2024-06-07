(ns whet.impl.template
  (:require
    [clojure.string :as string]
    [clojure.walk :as walk]
    [hiccup.core :as hiccup])
  (:import
    (clojure.lang MultiFn)))

(defn ^:private m->css [m]
  (if (map? m)
    (->> m
         (map (fn [[k v]] (str (name k) ": " v)))
         (string/join ";"))
    m))

(defn ^:private coll->class [class]
  (if (string? class)
    class
    (string/join " " (filter some? class))))

(defn ^:private clean-attrs [attrs]
  (into {}
        (remove (comp (some-fn fn? (comp string/blank? str)) val))
        (-> attrs
            (->> (walk/postwalk (fn [x]
                                  (cond
                                    (map-entry? x) (let [[k v] x]
                                                     [k (cond-> v (keyword? v) name)])
                                    (symbol? x) (name x)
                                    :else x))))
            (update :class coll->class)
            (update :style m->css))))

(defn ^:private expand* [expand-tree arg]
  (cond
    (vector? arg) (if (= :<> (first arg))
                    (map expand-tree (rest arg))
                    (expand-tree arg))
    (and (coll? arg) (sequential? arg)) (map expand-tree arg)
    (map? arg) (clean-attrs arg)
    :else arg))

(defn ^:private component? [node]
  (or (fn? node)
      (instance? MultiFn node)))

(defn expand-tree
  "Recursively expands a tree of reagent components into a hiccup tree."
  [[node & args :as tree]]
  (if (string? tree)
    tree
    (when-let [[node & args] (if-not (component? node)
                               tree
                               (loop [node (apply node args)]
                                 (if (component? node)
                                   (recur (apply node args))
                                   (expand-tree node))))]
      (into [node]
            (comp (map (partial expand* expand-tree))
                  (remove nil?))
            args))))

(defn into-template
  "Generates a hiccup template"
  [title store tree]
  [:html {:lang "en"}
   [:head
    [:title title]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "ie=edge"}]
    [:link {:rel "stylesheet" :href "/css/main.css"}]
    [:script {:type "application/javascript"}
     "window.WHET_INITIAL_DB = " (pr-str (pr-str @store))]
    [:script {:src "/js/main.js" :type "application/javascript" :defer true}]]
   [:body
    [:div#root
     tree]]])

(defn render
  "Renders a hiccup template to html"
  [template]
  (->> template
       hiccup/html
       (str "<!doctype html>")))
