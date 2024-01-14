(ns whet.defacto
  (:require
    [whet.reagent :as wr]
    [whet.dom :as wd]
    [defacto.core :as defacto]
    [defacto.resources.core :as res]))

(defn create
  ([request-fn nav]
   (create nil request-fn nav))
  ([ctx-map request-fn nav]
   (-> ctx-map
       (assoc :whet.core/nav nav)
       (res/with-ctx request-fn)
       (defacto/create wd/init-db {:->sub wr/ratom}))))

(defmethod defacto/query-responder :whet.core/?:route
  [db _]
  (::routing db))

(defmethod defacto/event-reducer :whet.core/navigated
  [db [_ routing-info]]
  (assoc db ::routing routing-info))
