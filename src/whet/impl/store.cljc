(ns whet.impl.store
  (:require
    #?(:cljs [pushy.core :as pushy])
    [clojure.edn :as edn]
    [clojure.set :as set]
    [defacto.core :as defacto]
    [defacto.resources.core :as res]
    [whet.interfaces :as iwhet]
    [whet.utils.dom :as wd]
    [whet.utils.reagent :as r]
    defacto.impl))

(deftype StubNav [route ^:volatile-mutable -store]
  defacto/IInitialize
  (init! [_ store]
    (set! -store store)
    (defacto/emit! -store [:whet.core/navigated route]))

  iwhet/INavigate
  (navigate! [_ token route-params query-params]
    (defacto/emit! -store [:whet.core/navigated {:token        token
                                                 :route-params route-params
                                                 :query-params query-params}]))
  (replace! [this token route-params query-params]
    (iwhet/navigate! this token route-params query-params)))

(def ^:private defacto-api
  {:command-handler defacto/command-handler
   :event-reducer   defacto/event-reducer
   :query-responder defacto/query-responder})

(defn ^:private ->Sub [atom-db query]
  (defacto.impl/->StandardSubscription atom-db query defacto/query-responder false))

(defn ^:private cljs-http->ring [handler]
  (fn [req]
    (try (let [response (-> req
                            (set/rename-keys {:url :uri})
                            (cond-> (string? (:body req)) (update :body edn/read-string))
                            handler)]
           (cond-> response
             (string? (:body response)) (update :body edn/read-string)))
         (catch #?(:cljs :default :default Throwable) ex
           (throw ex)))))

(defn success? [status]
  (and (integer? status)
       (<= 200 status 299)))

(defn ^:private ->request-fn [handler]
  (fn [_ params]
    (try
      (let [{:keys [status body]} (handler params)]
        (if (success? status)
          [::res/ok body]
          [::res/err body]))
      (catch #?(:cljs :default :default Throwable) ex
        [::res/err (-> ex ex-data :body)]))))

(defn create
  "Creates a reagent-compatible defacto store"
  [ctx-map request-fn opts]
  (-> ctx-map
      (res/with-ctx request-fn)
      (defacto/create wd/init-db (assoc opts :->sub r/atom))))

(defn hydrate-store
  "Creates and initializes a backend store for component hydration"
  [ctx-map route ui-handler]
  (let [nav (->StubNav route nil)
        handler (-> ui-handler cljs-http->ring ->request-fn)
        ctx (-> ctx-map
                (assoc :whet.core/nav nav)
                (res/with-ctx handler))]
    (doto (defacto.impl/->WatchableStore ctx (atom nil) defacto-api ->Sub)
      (->> (defacto/init! nav)))))

(defmethod defacto/query-responder :whet.core/?:route
  [db _]
  (::routing db))

(defmethod defacto/event-reducer :whet.core/navigated
  [db [_ routing-info]]
  (assoc db ::routing routing-info))
