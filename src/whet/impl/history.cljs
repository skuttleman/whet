(ns whet.impl.history
  (:require
    [defacto.core :as defacto]
    [pushy.core :as pushy]
    [whet.interfaces :as iwhet]
    [whet.utils.navigation :as nav]))

(deftype PushyNavigator [routes ^:volatile-mutable -pushy]
  iwhet/INavigate
  (navigate! [_ token route-params query-params]
    (pushy/set-token! -pushy (nav/path-for routes token route-params query-params)))
  (replace! [_ token route-params query-params]
    (pushy/replace-token! -pushy (nav/path-for routes token route-params query-params)))

  defacto/IInitialize
  (init! [_ store]
    (let [pushy (pushy/pushy #(defacto/emit! store [:whet.core/navigated %])
                             (partial nav/match routes))]
      (set! -pushy pushy)
      (pushy/start! pushy))))
