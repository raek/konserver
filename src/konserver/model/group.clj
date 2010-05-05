(ns konserver.model.group
  (:require [konserver
	     [db :as db]
	     [resource :as res]
	     [res-id :as id]
	     [uri :as uri]]
	    [konserver.model.common :as model])
  (:use clojure.contrib.error-kit
	konserver.model.error))

;;; The record for a group has the following keys:
;;;   :id        id
;;;   :name      human readable name

(defn- record->group [res-id r]
  (when r
    (with-meta
      {:name (:name r)
       :members (uri/for (id/member-coll res-id))
       :debts (uri/for (id/debt-coll res-id))}
      {:type (:type res-id)
       :res-id res-id})))

;;; Collection resource operations

(defmethod model/list ::res/group-coll
  [db res-id]
  (let [groups (sort-by :name (db/select (deref (:groups db)) {}))]
    (with-meta
      {:groups (vec (for [group groups]
		      {:uri (uri/for (id/group res-id (:id group)))
		       :name (:name group)}))}
      {:type (:type res-id)
       :res-id res-id
       :level :model})))

;;; TODO: add add

;;; Entity resource operations

(defmethod model/exists? ::res/group
  [db res-id]
  (boolean (seq (db/select (deref (:groups db))
			   {:id (:group res-id)}))))

(defmethod model/retrieve ::res/group
  [db res-id]
  (or (->> (db/select (deref (:groups db))
		      {:id (:group res-id)})
	   first
	   (record->group res-id))
      (raise does-not-exist-error (:group res-id))))

;;; TODO: add update

;;; TODO: add delete
