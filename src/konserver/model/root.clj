(ns konserver.model.root
  (:require [konserver
	     [resource :as res]
	     [res-id :as id]
	     [uri :as uri]]
	    [konserver.model.common :as model])
  (:use [konserver.config :only [*server-name*]]))

(defmethod model/retrieve ::res/root
  [db res-id]
  (with-meta
    {:name *server-name*
     :users (uri/for (id/user-coll res-id))
     :groups (uri/for (id/group-coll res-id))}
    {:type (:type res-id)
     :res-id res-id}))
