(ns whet.impl.defacto
  (:require
    [defacto.core :as defacto]))

(defmethod defacto/event-reducer :whet.core/in-env
  [db [_ env]]
  (assoc db :whet.core/env env))

(defmethod defacto/query-responder :whet.core/?:env
  [db _]
  (:whet.core/env db :prod))

(defmethod defacto/query-responder :whet.core/?:route
  [db _]
  (::routing db))

(defmethod defacto/event-reducer :whet.core/navigated
  [db [_ routing-info]]
  (assoc db ::routing routing-info))
