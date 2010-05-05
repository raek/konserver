(ns konserver.res-id
  (:require [konserver.resource :as res]))

(defn collection? [type]
  (isa? type ::res/collection))

(defn entity? [type]
  (isa? type ::res/entity))

(defn root? [type]
  (= type ::res/root))

(defn parent [id]
  (condp = (:type id)
    ::res/root nil
    ::res/user-coll (assoc id :type ::res/root)
    ::res/user (assoc (dissoc id :user) :type ::res/user-coll)
    ::res/group-coll (assoc id :type ::res/root)
    ::res/group (assoc (dissoc id :group) :type ::res/group-coll)
    ::res/member-coll (assoc id :type ::res/group)
    ::res/member (assoc (dissoc id :member) :type ::res/member-coll)
    ::res/debt-coll (assoc id :type ::res/group)
    ::res/debt (assoc (dissoc id :debt) :type ::res/debt-coll)
    ::res/debt-rev-coll (assoc id :type ::res/debt)
    ::res/debt-rev (assoc (dissoc id :revision) id :type ::res/debt-rev-coll)))

(defn child-type [coll-type]
  {:pre [(isa? coll-type ::res/collection)]
   :post [(isa? % ::res/entity)]}
  (condp = coll-type
    ::res/user-coll     ::res/user
    ::res/group-coll    ::res/group
    ::res/member-coll   ::res/member
    ::res/debt-coll     ::res/debt
    ::res/debt-rev-coll ::res/debt-rev
    ::res/debug-coll    ::res/debug))

(defn path [id]
  (if (= (:type id) ::res/root)
    [id]
    (conj (path (parent id)) id)))

(defn user-coll [base]
  (assoc base
    :type ::res/user-coll))

(defn user [base id]
  (assoc base
    :type ::res/user
    :user id))

(defn group-coll [base]
  (assoc base
    :type ::res/group-coll))

(defn group [base id]
  (assoc base
    :type ::res/group
    :group id))

(defn member-coll [base]
  (assoc base
    :type ::res/member-coll))

(defn member [base id]
  (assoc base
    :type ::res/member
    :member id))

(defn debt-coll [base]
  (assoc base
    :type ::res/debt-coll))

(defn debt [base id]
  (assoc base
    :type ::res/debt
    :debt id))

(defn debt-rev-coll [base]
  (assoc base
    :type ::res/debt-rev-coll))

(defn debt-rev [base id]
  (assoc base
    :type ::res/debt-rev
    :revision id))
