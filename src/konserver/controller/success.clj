(ns konserver.controller.success
  (:require [konserver
	     [resource :as res]
	     [uri :as uri]]))

(defn deleted [parent-res-id]
  (let [uri (uri/for parent-res-id)]
    (with-meta
      {:msg (str "The resource has been deleted from the collection: " uri)
       :collection uri}
      {:type ::res/deleted})))

(defn created [res-id]
  (let [uri (uri/for res-id)]
    (with-meta
      {:uri uri}
      {:type ::res/created
       :res-id res-id
       :code 201
       :headers {"Location" uri}
       :name "Created"})))

(defn no-content []
  (with-meta
    {}
    {:type ::res/no-content
     :code 204
     :name "No Content"}))
