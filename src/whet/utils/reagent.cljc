(ns whet.utils.reagent
  #?(:cljs (:require-macros whet.utils.reagent))
  (:require
    [reagent.core :as r]))

(def ^{:arglists '([spec])} create-class
  #?(:cljs    r/create-class
     :default :reagent-render))

(def ^{:arglists '([value])} ratom
  #?(:cljs    r/atom
     :default atom))

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
