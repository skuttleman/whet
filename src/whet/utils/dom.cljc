(ns whet.utils.dom
  (:require
    [clojure.edn :as edn]))

(def ^:const window
  #?(:cljs js/window :default nil))

(def ^:const init-db
  #?(:cljs    (some-> window .-WHET_INITIAL_DB edn/read-string)
     :default {}))

(defn prevent-default! [e]
  (doto e
    #?(:cljs
       (some-> .preventDefault))))

(defn stop-propagation! [e]
  (doto e
    #?(:cljs
       (some-> .stopPropagation))))

(defn target-value [e]
  #?(:cljs
     (some-> e .-target .-value)))

(defn blur! [node]
  (doto node
    #?(:cljs
       (some-> .blur))))

(defn focus! [node]
  (doto node
    #?(:cljs
       (some-> .focus))))
