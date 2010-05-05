(ns konserver.model.user
  (:require (konserver [db :as db]
		       [resource :as res]
		       [res-id :as id]
		       [uri :as uri])
	    [konserver.model.common :as model])
  (:use clojure.contrib.error-kit
	[konserver.util :only [boolean? md5]]
	[konserver.date :only [now]]
	[konserver.crypto :only [make-password-hash-and-salt]]
	konserver.model.error))

;;; Attributes

(def known-attrs #{:nick :password :added :updated :role :deleted :name :email :email-md5})
(def read-only-add-attrs #{:added :updated :email-md5})
(def read-only-update-attrs #{:nick :added :updated :email-md5})
(def required-add-attrs #{:nick :name :password})
(def default-attrs {:role :user, :deleted false, :email nil})
(def attr-types
     {:nick "string"
      :role "keyword"
      :deleted "boolean"
      :name "string"
      :email "string or null"})
(def attr-validators
     {:nick string?
      :role keyword?
      :deleted boolean?
      :name string?
      :email #(or (nil? %) (string? %))})

(defn- record->user [res-id r]
  (when r
    (with-meta
      (-> r
	  (dissoc :id :password-hash :password-salt)
	  (assoc :nick (name (:id r))
		 :email-md5 (when (:email r)
			      (md5 (:email r)))))
      {:type (:type res-id)
       :res-id res-id})))

;;; Validators

(def check-known-attrs #(check-known-attrs* % known-attrs))
(def check-read-only-add-attrs #(check-read-only-attrs* % read-only-add-attrs))
(def check-read-only-update-attrs #(check-read-only-attrs* % read-only-update-attrs))
(def check-required-add-attrs #(check-required-attrs* % required-add-attrs))
(def check-attr-types #(check-attr-types* % attr-validators attr-types))

;;; Collection resource operations

(defmethod model/list ::res/user-coll
  [db res-id]
  (let [users (sort-by :name (db/select (deref (:users db))
					{:deleted false}))]
    (with-meta
      {:users (vec (for [user users]
		     {:uri (uri/for (id/user res-id (:id user)))
		      :nick (name (:id user))
		      :name (:name user)
		      :email-md5 (when (:email user)
				   (md5 (:email user)))}))}
      {:type (:type res-id)
       :res-id res-id})))

(defmethod model/add ::res/user-coll
  [db res-id user _]
  (-> user check-known-attrs check-required-add-attrs
      check-read-only-add-attrs check-attr-types)
  (let [user-id (keyword (:nick user))]
    (dosync
     (when (model/exists? db (id/user res-id user-id))
       (raise already-exists-error user-id))
     (let [time-now (now)
	   time-attrs {:added time-now, :updated time-now}
	   id-attr {:id user-id}
	   [password-hash password-salt]
	   (make-password-hash-and-salt (:password user))
	   password-attrs {:password-hash password-hash,
			   :password-salt password-salt}
	   user-prime (merge default-attrs
			     id-attr
			     time-attrs
			     password-attrs
			     (dissoc user :nick :password))]
       (alter (:users db) db/insert user-prime)
       (id/user res-id user-id)))))

;;; Entity resource operations

(defmethod model/exists? ::res/user
  [db res-id]
  (boolean (seq (db/select (deref (:users db))
			   {:id (:user res-id)}))))

(defmethod model/retrieve ::res/user
  [db res-id]
  (or (->> (db/select (deref (:users db))
		      {:id (:user res-id)})
	   first
	   (record->user res-id))
      (raise does-not-exist-error (:user res-id))))

(defmethod model/update ::res/user
  [db res-id updates _]
  (-> updates check-known-attrs check-read-only-update-attrs
      check-attr-types)
  (dosync
   (when-not (model/exists? db res-id)
     (raise does-not-exist-error (:user res-id)))
   (alter (:users db) db/update
	  {:id (:user res-id)}
	  (assoc updates :updated (now)))
   (model/retrieve db res-id)))

(defmethod model/delete ::res/user
  [db res-id _]
  (dosync
   (when-not (model/exists? db res-id)
     (raise does-not-exist-error (:user res-id)))
   (when (:deleted (model/retrieve db res-id))
     (raise already-deleted-error (:user res-id)))
   (alter (:users db) db/update {:id (:user res-id)} {:deleted true})
   nil))
