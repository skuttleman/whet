(ns whet.interfaces)

(defprotocol INavigate
  ""
  (navigate! [this token route-params query-params]
    "")
  (replace! [this token route-params query-params]
    ""))

(defmulti ^{:arglists '([token params])} coerce-route-params
          ""
          (fn [token _] token))
(defmethod coerce-route-params :default [_ params] params)
