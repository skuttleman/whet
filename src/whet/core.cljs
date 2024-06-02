(ns whet.core
  (:require
    [reagent.dom :as rdom]
    [whet.impl.store :as store]
    [whet.interfaces :as iwhet]
    [whet.impl.history :as hist]
    whet.impl.defacto
    whet.impl.http))

(defn render
  "mount a reagent view to the root of the app and render it"
  ([view]
   (render view (constantly nil)))
  ([view cb]
   (rdom/render view (.getElementById js/document "root") cb)))

(defn with-ctx [ctx-map routes]
  (assoc ctx-map ::routes routes ::nav (hist/->PushyNavigator routes nil)))

(defn render-ui
  "creates a store, then mounts and renders the component with a store"
  ([ctx-map store->component]
   (render-ui ctx-map store->component nil))
  ([ctx-map store->component opts]
   (-> ctx-map
       (store/create #(iwhet/handle-request %1 ctx-map %2) opts)
       store->component
       (render (:cb opts (constantly nil))))))
