(ns whet.core
  (:require
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
        tree (tmpl/expand-tree (cb store))]
    (tmpl/into-template store tree)))

(defn with-html-heads
  ""
  [template & heads]
  (update template 2 into heads))

(defn render-template
  ""
  [template]
  (tmpl/render template))
