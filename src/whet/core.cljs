(ns whet.core
  (:require
    [reagent.dom :as rdom]
    [whet.impl.store :as store]
    [whet.interfaces :as iwhet]
    [whet.impl.history :as hist]
    whet.impl.http))

(defn render
  "mount a reagent view to the root of the app and render it"
  ([view]
   (render view (constantly nil)))
  ([view cb]
   (rdom/render view (.getElementById js/document "root") cb)))

(defn render-ui
  "creates a store, then mounts and renders the component with a store"
  ([routes store->component]
   (render-ui routes store->component (constantly nil)))
  ([routes store->component cb]
   (-> (store/create #(iwhet/handle-request %1 routes %2)
                     (hist/->PushyNavigator routes nil))
       store->component
       (render cb))))
