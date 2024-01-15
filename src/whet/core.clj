(ns whet.core
  (:require
    [defacto.core :as defacto]
    [defacto.resources.core :as res]
    [whet.impl.store :as store]
    [whet.impl.middleware :as mw]
    [whet.impl.template :as tmpl]))

(def ^{:arglists '([be-handler routes])} with-middleware
  "Wrap your backend handler in this middleware when mounting your app."
  mw/with-middleware)

(defn into-template
  "Takes "
  [route ui-handler cb]
  (let [store (store/hydrate-store route ui-handler)
        tree (cb store)
        resources (defacto/subscribe store [::res/?:resources])]
    (tmpl/expand-tree tree)
    (while (some res/requesting? @resources)
      (Thread/sleep 1))
    (tmpl/into-template store (tmpl/expand-tree tree))))

(defn with-html-heads
  ""
  [template & heads]
  (update template 2 into heads))

(defn render-template
  ""
  [template]
  (tmpl/render template))
