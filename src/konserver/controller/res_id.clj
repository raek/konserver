(ns konserver.controller.res-id
  (:require [konserver.controller.error :as error]))

(defn with-resource-id [handler parse-uri]
  (fn [request]
    (if-let [res-id (parse-uri (get request :konserver.controller/uri))]
      (handler (assoc request :konserver.controller/res-id res-id))
      (error/not-found (get request :konserver.controller/uri)))))
