(ns konserver.model.debt
  (:require (konserver [db :as db]
		       [date :as date]
		       [resource :as res]
		       [res-id :as id]
		       [uri :as uri])
	    [konserver.model.common :as model])
  (:use clojure.contrib.error-kit  
	[konserver.util :only (boolean? update update-maybe)]
	[konserver.date :only (now)]
	(konserver.model error
			 [member :only (increase-balance decrease-balance)]
			 [math
			  :only (round-half-to-even)
			  :rename {round-half-to-even round}])))

;;; The records in the debts table has the following keys (the records
;;; actually more closely correspond to debt revisions):
;;;
;;; :id*, :rev*, :current*, :added, :updated, :deleted, :editor, :comment,
;;; :name, :amount, :description, :creditor, :debtors
;;;
;;; The keys marked with asterisks are not directly visible as debt
;;; attributes.  All debt attributes (except for :current-revision) correspond
;;; to the keys with the same name in a record, which is the one that has the
;;; :id of the debt and :current set to true. :current-reverion corresponds
;;; to :rev of that record.

;;; Attributes

(def known-attrs
     #{:current-revision :added :updated :deleted :editor :comment
       :name :amount :to-creditor :from-debtor
       :description :creditor :debtors})

(def read-only-add-attrs
     #{:current-revision :added :updated :editor :to-creditor :from-debtor})

(def read-only-update-attrs
     #{:added :updated :editor :to-creditor :from-debtor})

(def required-add-attrs
     #{:comment :name :amount :creditor :debtors})

(def required-update-attrs
     #{:current-revision :comment})

(def attr-defaults
     {:deleted false, :description nil})

(def attr-type-names
     {:current-revision "revision uri"
      :deleted "boolean"
      :comment "string"
      :name "string"
      :amount "positive integer"
      :description "string or nil"
      :creditor "user uri"
      :debtors "set of user uris"})

(def attr-validators
     {:current-revision string?
      :deleted boolean?
      :comment #(and (string? %) (seq %))
      :name string?
      :amount #(and (integer? %) (pos? %))
      :description #(or (nil? %) (string? %))
      :creditor string?
      :debtors #(and (set? %) (every? string? %))})

;;; Validators

(def check-known-attrs
     #(check-known-attrs* % known-attrs))
(def check-read-only-add-attrs
     #(check-read-only-attrs* % read-only-add-attrs))
(def check-read-only-update-attrs
     #(check-read-only-attrs* % read-only-update-attrs))
(def check-required-add-attrs
     #(check-required-attrs* % required-add-attrs))
(def check-required-update-attrs
     #(check-required-attrs* % required-update-attrs))
(def check-attr-types
     #(check-attr-types* % attr-validators attr-type-names))

(defn check-valid-user [db user-id attr]
  (when-not (and (= (:type user-id) ::res/user)
		 (model/exists? db user-id))
    (raise invalid-uri-error attr (uri/for user-id))))

(defn check-valid-member [db user-id debt-coll-id attr]
  (check-valid-user db user-id attr)
  (when-not (model/exists? db (id/member debt-coll-id (:user user-id)))
    (raise not-a-member-error attr (uri/for user-id))))

(defn check-valid-revision [db debt-rev-id debt-id attr]
  (when-not (= (:type debt-rev-id) ::res/debt-rev)
    (raise invalid-uri-error attr (uri/for debt-rev-id))))

;;; Accounting helper functions

(defn commit-debt
  [db res-id {:keys [amount creditor debtors], :as debt}]
  "Modifies the balance of the creditor the and debtors of the debt so
   that the debt has been taken into account."
  (let [debtor-count (count debtors)
	from-debtor  (round (/ amount debtor-count))
	to-creditor  (* from-debtor debtor-count)]
    (increase-balance db (id/member res-id creditor) to-creditor)
    (doseq [debtor debtors]
      (decrease-balance db (id/member res-id debtor) from-debtor))))

(defn rollback-debt
  [db res-id debt]
  "Undoes the effect of a corresponding apply-debt."
  (commit-debt db res-id (update debt :amount -)))

;;; Collection resource operations

(defmethod model/list ::res/debt-coll
  [db res-id]
  (let [debts (db/select (deref (:debts db))
				{:current true :deleted false})
	debts (sort-by :id debts)]
    (with-meta
      {:debts (vec
	       (reverse
		(for [{:keys [id rev added updated name amount creditor debtors]} debts
		      :let [debtor-count (count debtors)
			    from-debtor (round (/ amount debtor-count))
			    to-creditor (* from-debtor debtor-count)]]
		  {:uri (uri/for (id/debt res-id id))
		   :added added
		   :updated updated
		   :revisions (uri/for (id/debt-rev-coll (id/debt res-id id)))
		   :current-revision (uri/for (id/debt-rev (id/debt res-id id)
							   rev))
		   :name name
		   :amount amount
		   :creditor (uri/for (id/user res-id creditor))
		   :debtors (into (empty debtors)
				  (map #(uri/for (id/user res-id %)) debtors))
		   :to-creditor to-creditor
		   :from-debtor from-debtor})))}
      {:type (:type res-id)
       :res-id res-id})))

(defmethod model/add ::res/debt-coll
  [db res-id debt editor]
  (-> debt check-known-attrs check-required-add-attrs
      check-read-only-add-attrs check-attr-types)
  (let [debt (update debt ; uri -> res-id
		     :creditor uri/parse
		     :debtors #(into (empty %)
				     (map uri/parse %)))
	_ (do (check-valid-member db (:res-id (meta editor)) res-id :editor)
	      (doseq [user (conj (:debtors debt) (:creditor debt))]
		(check-valid-member db user res-id :creditor)))
	debt (update debt ; res-id -> internal id
		     :creditor :user
		     :debtors #(into (empty %)
				     (map :user %)))]
    (dosync
     (let [id (deref (:next-debt-id db))
	   id-attrs {:id id, :rev 1, :current true}
	   time-now (now)
	   edit-attrs {:editor (:user (:res-id (meta editor))), :added time-now, :updated time-now}
	   debt-record (merge attr-defaults id-attrs edit-attrs debt)]
       (alter (:next-debt-id db) inc)
       (alter (:debts db) db/insert debt-record)
       (commit-debt db res-id debt-record)
       (id/debt res-id id)))))

;;; Entity resource operations

(defmethod model/exists? ::res/debt
  [db res-id]
  (boolean (seq (db/select (deref (:debts db))
			   {:id (:debt res-id)}))))

(defmethod model/retrieve ::res/debt
  [db res-id & options]
  (let [options (apply hash-map options)
	raise-if-not-found (= (get options :not-found :error))]
    (if-let [{:keys [id rev added updated editor comment
		     name amount description creditor debtors]}
	     (first (db/select (deref (:debts db))
			       {:id (:debt res-id)
				:current true}))]
      (let [debtor-count (count debtors)
	    from-debtor (round (/ amount debtor-count))
	    to-creditor (* from-debtor debtor-count)]
	(with-meta
	  {:uri (uri/for (id/debt res-id id))
	   :added added
	   :updated updated
	   :editor (uri/for (id/user res-id editor))
	   :comment comment
	   :revisions (uri/for (id/debt-rev-coll (id/debt res-id id)))
	   :current-revision (uri/for (id/debt-rev (id/debt res-id id) rev))
	   :name name
	   :amount amount
	   :description description
	   :creditor (uri/for (id/user res-id creditor))
	   :debtors (into (empty debtors)
			  (map #(uri/for (id/user res-id %)) debtors))
	   :to-creditor to-creditor
	   :from-debtor from-debtor}
	  {:type (:type res-id)
	   :res-id res-id}))
      (when raise-if-not-found
	(raise does-not-exist-error (:debt res-id))))))

 (defmethod model/update ::res/debt
   [db res-id updates editor]
   (-> updates check-known-attrs check-required-update-attrs
       check-read-only-update-attrs check-attr-types)
   (dosync
    (let [updates (update-maybe updates ; uri -> res-id
				:current-revision uri/parse
				:creditor uri/parse
				:debtors #(into (empty %)
						(map uri/parse %)))
	  _ (do (check-valid-revision db (:current-revision updates) res-id
				      :current-revision)
		(check-valid-member db (:res-id (meta editor)) res-id :editor)
		(when (contains? updates :creditor)
		  (check-valid-member db (:creditor updates) res-id :debtor))
		(when (contains? updates :debtors)
		  (doseq [user (:debtors updates)]
		    (check-valid-member db user res-id :debtors))))
	  updates (update-maybe updates ; res-id -> internal id
				:current-revision :revision
				:creditor :user
				:debtors #(into (empty %)
						(map :user %)))]
      (if-let [latest (first (db/select (deref (:debts db))
					{:id (:debt res-id) :current true}))]
	(if-not (= (:rev latest) (:current-revision updates))
	  (raise update-conflict-error)
	  (let [debt-record (merge latest
				   (dissoc updates :current-revison)
				   {:rev (inc (:rev latest))
				    :updated (now)
				    :current true
				    :editor (:user (:res-id (meta editor)))})]
	    (alter (:debts db) db/update 
		   {:id (:debt res-id) :rev (:rev latest)} {:current false})
	    (alter (:debts db) db/insert debt-record)
	    (rollback-debt db res-id latest)
	    (commit-debt db res-id debt-record)
	    (model/retrieve db res-id)))
	(raise does-not-exist-error (:debt res-id))))))

(defmethod model/delete ::res/debt
  [db res-id editor]
  (dosync
   (if-let [latest (first (db/select (deref (:debts db))
				     {:id (:debt res-id) :current true}))]
     (if (:deleted latest)
       (raise already-deleted-error (:debt res-id))
       (let [debt-record (merge latest
				{:rev (inc (:rev latest))
				 :updated (now)
				 :current true
				 :deleted true
				 :comment "deleted"})]
	 (alter (:debts db) db/update
		{:id (:debt res-id) :rev (:rev latest)} {:current false})
	 (alter (:debts db) db/insert debt-record)))
     (raise does-not-exist-error (:debt res-id)))))
