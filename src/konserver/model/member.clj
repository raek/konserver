(ns konserver.model.member
  (:require [konserver
	     [db :as db]
	     [date :as date]
	     [resource :as res]
	     [res-id :as id]
	     [uri :as uri]]
	    [konserver.model user
	     [common :as model]])
  (:use clojure.contrib.error-kit
	[konserver.date :only [now]]
	[konserver.model error]))

;;; The record for a member has the following keys:
;;;   :user      id of the user that is the member
;;;   :added     timestamp of when the member joined the group
;;;   :updated   timestamp of the latest balance change
;;;   :current-balance  the current balance of the member in the group

;; FIXME: only users in the users table should be able to have a balance

;;; Attributes

(def known-attrs #{:user :added :updated :current-balance})
(def read-only-add-attrs #{:added :updated :current-balance})
(def required-add-attrs #{:user})
(def attr-types {:user "user uri"})
(def attr-validators {:user string?})

;;; Validators

(def check-known-attrs #(check-known-attrs* % known-attrs))
(def check-required-add-attrs #(check-required-attrs* % required-add-attrs))
(def check-read-only-add-attrs #(check-read-only-attrs* % read-only-add-attrs))
(def check-attr-types #(check-attr-types* % attr-validators attr-types))

;;; Collection resource operations

(defmethod model/list ::res/member-coll
  [db res-id]
  (dosync
   (let [member-records (db/select (deref (:members db)) {})
	 users (for [member member-records]
		 (model/retrieve db (id/user res-id (:user member))))
	 members (map (fn [member user]
			{:uri (uri/for (id/member res-id (:user member)))
			 :user (uri/for (:res-id (meta user)))
			 :updated (:updated member)
			 :current-balance (:current-balance member)
			 :nick (:nick user)
			 :name (:name user)
			 :email-md5 (:email-md5 user)})
		      member-records users)
	 members (sort-by :name members)]
     (with-meta
       {:members (vec members)
	:updated (when (seq members)
		   (reduce date/latest (map :updated members)))}
       {:type (:type res-id)
	:res-id res-id}))))

(defmethod model/add ::res/member-coll
  [db res-id member _]
  (-> member check-known-attrs check-required-add-attrs
      check-read-only-add-attrs check-attr-types)
  (if-let [user-res-id (uri/parse (:user member))]
    (dosync
     (when (model/exists? db (id/member res-id (:user user-res-id)))
       (raise already-exists-error (:user user-res-id)))
     (let [time-now (now)
	   member-record {:user (:user user-res-id)
			  :added time-now
			  :updated time-now
			  :current-balance 0}]
       (alter (:members db) db/insert member-record)
       (id/member res-id (:user user-res-id))))
    (raise invalid-uri-error :user (:user member))))

;;; Entity resource operations

(defmethod model/exists? ::res/member
  [db res-id]
  (boolean (seq (db/select (deref (:members db))
			   {:user (:member res-id)}))))

(defmethod model/retrieve ::res/member
  [db res-id]
  (if-let [member-record (first (db/select (deref (:members db))
					   {:user (:member res-id)}))]
    (let [user (model/retrieve db (id/user res-id (:user member-record)))]
      (with-meta
	(assoc member-record
	  :user (uri/for (:res-id (meta user)))
	  :nick (:nick user)
	  :name (:name user)
	  :email-md5 (:email-md5 user))
	{:type (:type res-id)
	 :res-id res-id}))
    (raise does-not-exist-error (:member res-id))))

;;; No update method since members can only be indirectly modified

(defmethod model/delete ::res/member
  [db res-id _]
  (dosync
   (when-not (model/exists? db res-id)
     (raise does-not-exist-error (:member res-id)))
   (let [member (model/retrieve db res-id)]
     (when-not (zero? (:current-balance member))
       (raise balance-not-zero-error (:member res-id)
	      (:current-balance member))))
   (alter (:members db) db/delete {:user (:member res-id)})
   nil))

;;; Accounting helper functions

(defn increase-balance [db member-id delta]
  (let [old (:current-balance
	     (model/retrieve db member-id))]
    (alter (:members db) db/update
	   {:user (:member member-id)}
	   {:current-balance (+ old delta)
	    :updated (date/now)})))

(defn decrease-balance [db member-id delta]
  (increase-balance db member-id (- delta)))
