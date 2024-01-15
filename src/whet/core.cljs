(ns whet.core
  (:require
    [reagent.dom :as rdom]
    [whet.impl.store :as store]
    [whet.interfaces :as iwhet]
    [whet.impl.history :as hist]
    whet.impl.http))

(defn render
  ""
  ([view]
   (render view (constantly nil)))
  ([view cb]
   (rdom/render view (.getElementById js/document "root") cb)))

(defn render-ui
  ""
  ([routes component]
   (render-ui routes component (constantly nil)))
  ([routes component cb]
   (let [[component & args] (cond-> component (not (vector? component)) vector)
         store (store/create #(iwhet/handle-request %1 routes %2)
                             (hist/->PushyNavigator routes nil))]
     (render (into [component store] args) #(cb store)))))
