(ns whet.core
  "A framework for hydration"
  (:require
    #?@(:clj  [[whet.middleware :as mw]]
        :cljs [[reagent.dom :as rdom]
               [whet.http :as http]])
    [whet.defacto :as store]
    [whet.navigation :as nav]))

#?(:clj
   (def ^{:arglists '([be-handler routes])} with-base-middleware
     mw/with-base))

#?(:clj
   (def ^{:arglists '([handler ui-handler] [handler ui-handler opts])} with-hydration-middleware
     mw/with-hydration))

#?(:cljs
   (defn render-ui
     ([routes component]
      (render-ui routes component (constantly nil)))
     ([routes component cb]
      (let [[component & args] (cond-> component (not (vector? component)) vector)
            store (store/create http/request-fn
                                (nav/->PushyNavigator routes nil))]
        (let [root (.getElementById js/document "root")]
          (rdom/render (into [component store] args) root #(cb store)))))))
