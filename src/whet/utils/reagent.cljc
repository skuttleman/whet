(ns whet.utils.reagent
  "A wrapper for using reagent in cljc"
  #?(:cljs (:require-macros whet.utils.reagent))
  (:refer-clojure :exclude [atom])
  (:require
    [reagent.core :as r]))

(def ^{:arglists '([spec])} create-class
  #?(:cljs    r/create-class
     :default :reagent-render))

(def ^{:arglists '([this])} argv
  #?(:cljs    r/argv
     :default (constantly nil)))

(def ^{:arglists '([value])} atom
  #?(:cljs    r/atom
     :default clojure.core/atom))

(defmacro with-let [bindings & body]
  (let [final-form (last body)
        [body fin] (if (and (list? final-form) (= 'finally (first final-form)))
                     [(butlast body) (rest final-form)]
                     [body nil])]
    (if (:ns &env)
      `(r/with-let ~bindings
                    ~@body
                    ~(list 'finally
                           `(do ~@fin)))
      `(let ~bindings (try ~@body)))))
