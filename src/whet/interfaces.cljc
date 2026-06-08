(ns whet.interfaces)

(defprotocol INavigate
  "An interface for modifying the browser's history"
  (navigate! [this token route-params query-params]
    "pushes state to the browser history")
  (replace! [this token route-params query-params]
    "replaces the current state in the browser's history"))

(defmulti ^{:arglists '([spec-key ctx-map params])} handle-request
          "extend this multimethod to support defacto resource
           request-fn other than the http client provided"
          (fn [_ _ {type :whet.core/type}]
            (cond-> type (vector? type) first)))
