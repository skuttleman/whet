(ns whet.core
  (:require
    [defacto.core :as defacto]
    [defacto.resources.core :as res]
    [whet.impl.store :as store]
    [whet.impl.middleware :as mw]
    [whet.impl.template :as tmpl]
    whet.impl.defacto))

(def ^{:arglists '([be-handler routes])} with-middleware
  "Wrap your backend handler AND your ui handler in this middleware when mounting your app."
  mw/with-middleware)

(defn into-template
  "Creates a store and generates an expanded hiccup template"
  [ctx-map route ui-handler store->reagent-tree]
  (let [store (store/hydrate-store ctx-map route ui-handler)
        tree (store->reagent-tree store)
        resources (defacto/subscribe store [::res/?:resources])]
    (tmpl/expand-tree tree)
    (while (some res/requesting? @resources)
      (Thread/sleep 1))
    (tmpl/into-template store (tmpl/expand-tree tree))))

(defn with-html-heads
  "Add additional hiccup nodes to the template's <head> section"
  [template & heads]
  (update template 2 into heads))

(defn render-template
  "renders the HTML template"
  [template]
  (tmpl/render template))
