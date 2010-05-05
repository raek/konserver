(ns konserver.model
  "Access to the konserver model
   
   The model operations take the following arguments: 'db', 'res-id' and
   (possibly) 'data'. 'db' is the database, which is a map of the tables for
   the collections. 'res-id' is a resource identifier, which identifies a
   resource within the database. 'data' is used for representing queries for
   'list', and for updated information for 'update'"
  (:refer-clojure :exclude [list])
  (:require [konserver
	     [config :as config]
	     [db :as db]
	     [resource :as res]]
	    [konserver.model common root user group member debt
	     [persistence :as persist]])
  (:use [clojure.contrib.ns-utils :only [immigrate]]
	clojure.contrib.error-kit
	konserver.model.error
	[konserver.config :only [*data-directory* *groups*]]
	konserver.crypto)
  (:import java.io.File))

(immigrate 'konserver.model.common)

(defn group-directory [group]
  (File. *data-directory* (name (:id group))))

(defn file-path [path]
  (reduce #(File. %1 %2) path))

(defn table-path [group table-id]
  (file-path [*data-directory*
	      (name (:id group))
	      (str (name table-id) ".clj")]))

(defn load-users []
  (ref (persist/load (file-path [*data-directory* "users.clj"]))))

(defn store-users [users]
  (persist/store (file-path [*data-directory* "users.clj"]) (deref users)))

(defn load-group [group]
  (into {:id (:id group)
	 :name (:name group)}
	(for [table-id [:members :next-debt-id :debts]]
	  [table-id (-> (table-path group table-id) persist/load ref)])))

(defn store-group [group]
  (let [tables (for [[key val] group
		     :when (contains? #{:members :next-debt-id :debts} key)]
		 [key (deref val)])]
    (doseq [[table-id table-data] tables]
      (persist/store (table-path group table-id) table-data))))

(defn load-groups []
  (ref (reduce #(db/insert %1 %2) (db/make-table)
	       (map load-group *groups*))))

(defn store-groups [groups]
  (doseq [group (deref groups)] (store-group group)))

(defn load-all []
  {:users (load-users)
   :groups (load-groups)})

(defn store-all [{users :users groups :groups}]
  (store-users users)
  (store-groups groups))

(def all-data (ref (load-all)))

(defn select-db [res-id]
  (let [data @all-data
	db {:users (:users data)
	    :groups (:groups data)}
	db (if (contains? res-id :group)
	     (let [group (first (db/select (deref (:groups db))
					   {:id (:group res-id)}))]
		(when-not group
		  (raise does-not-exist-error (:group res-id)))
		(merge db {:group-name (:name group)
			   :members (:members group)
			   :debts (:debts group)
			   :next-debt-id (:next-debt-id group)}))
	     db)]
    db))

(defn authenticate-user [nick password]
  (when-let [user (first (db/select (-> all-data deref :users deref)
				    {:id (keyword nick)}))]
    (let [user (with-meta user {:type ::res/user,
				:res-id {:type ::res/user
					 :user (keyword nick)}})]
      (when (valid-password? password
			     (:password-hash user)
			     (:password-salt user))
	user))))
