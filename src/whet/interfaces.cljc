(ns whet.interfaces)

(defprotocol INavigate
  "An interface for modifying the browser's history"
  (navigate! [this token route-params query-params]
    "pushes state to the browser history")
  (replace! [this token route-params query-params]
    "replaces the current state in the browser's history"))

(defmulti ^{:arglists '([token params])} coerce-route-params
          "coerce route params for a token"
          (fn [token _] token))
(defmethod coerce-route-params :default [_ params] params)

(defmulti ^{:arglists '([spec-key routes params])} handle-request
          "extend this multimethod to support defacto resource
           request-fn other than the http client provided"
          (fn [spec _ _] spec))
