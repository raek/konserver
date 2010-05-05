(ns konserver.view.form
  (:require [konserver.resource :as res]
	    [konserver.view.common :as view])
  (:use [konserver.util :only [update-maybe]]))

(defn- parse-boolean [s]
  (condp = (.toLowerCase s)
      "true" true
      "false" false
      s))

(defn- parse-integer [s]
  (if (re-find #"^[0-9]+$" s)
    (binding [*read-eval* false]
      (read-string s))
    s))

(defmethod view/parse-form :default
  [form-map type]
  form-map)

(defmethod view/parse-form ::res/user
  [form-map _]
  (update-maybe form-map :deleted parse-boolean))

(defn- debtor-attr? [attr]
  (.startsWith (name attr) "debtor"))

(defmethod view/parse-form ::res/debt
  [form-map type]
  (let [debtors  (map form-map (filter debtor-attr? (keys form-map)))]
    (-> form-map
	(select-keys (remove debtor-attr? (keys form-map)))
	(assoc :debtors (set debtors))
	(update-maybe :amount parse-integer))))
  