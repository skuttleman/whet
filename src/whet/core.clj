(ns whet.core
  (:require
    [defacto.core :as defacto]
    [defacto.resources.core :as res]
    [whet.impl.store :as store]
    [whet.impl.middleware :as mw]
    [whet.impl.template :as tmpl]))

(def ^{:arglists '([be-handler routes])} with-middleware
  ""
  mw/with-middleware)

(defn into-template
  ""
  [route ui-handler cb]
  (let [store (store/hydrate-store route ui-handler)
        tree (tmpl/expand-tree (cb store))
        resources (defacto/subscribe store [::res/?:resources])]
    (while (some res/requesting? @resources)
      (Thread/sleep 1))
    (tmpl/into-template store tree)))

(defn with-html-heads
  ""
  [template & heads]
  (update template 2 into heads))

(defn render-template
  ""
  [template]
  (tmpl/render template))
