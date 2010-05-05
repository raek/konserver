(ns konserver.model.error
  "Errors for model operations"
  (:use [clojure.contrib.error-kit]))

(deferror model-error []
  "Base error for model operations"
  [])

(deferror already-exists-error [model-error]
  "An entity with the id already exists in the collection"
  [id]
  {:msg (format "An entity with the id already exists in the collection: %s" id)
   :id id})

(deferror does-not-exist-error [model-error]
  "An entity with the id does not exist in the collection"
  [id]
  {:msg (format "An entity with the id does not exists in the collection: %s" id)
   :id id})

(deferror update-conflict-error [model-error]
  "An update cannot be performed because the resource has been changed"
  []
  {:msg "The entity cannot be updated because it has been changed."})

(deferror already-deleted-error [model-error]
  "The entity with the id has already been deleted from the collection"
  [id]
  {:msg (format "The entity has already been deleted from the collection: %s" id)
   :id id})

(deferror balance-not-zero-error [model-error]
  "The membership of a user cannot be revoked if the user's balance is non-zero"
  [id balance]
  {:msg (format "The membership for the user %s cannot be revoked since the user's balance is non-zero: %d"
		id balance)
   :id id
   :balance balance})

(deferror attr-error [model-error]
  "Base error for attribute validation errors"
  [])

(deferror unknown-attr-error [attr-error]
  "Encountered an unknown attribute"
  [attr]
  {:msg (format "Unknown attribute: %s" (name attr))
   :attr attr})

(deferror missing-attr-error [attr-error]
  "Required attribute missing"
  [attr]
  {:msg (format "Missing required attribute: %s" (name attr))
   :attr attr})

(deferror read-only-attr-error [attr-error]
  "Tried to change a read-only attribute"
  [attr]
  {:msg (format "Cannot change read-only attribute: %s" (name attr))
   :attr attr})

(deferror type-error [attr-error]
  "Attribute value is of wrong type"
  [attr expected actual]
  {:msg (format "Value of attribute '%s' is of wrong type: expected %s, got %s"
		(name attr) expected actual)
   :attr attr
   :expected expected
   :actual actual})

(deferror invalid-uri-error [attr-error]
  [attr uri]
  {:msg (format "Invalid URI in attribute '%s': \"%s\"" (name attr) uri)
   :attr attr
   :uri uri})

(deferror wrong-group-error [attr-error]
  [attr uri]
  {:msg (format "The resource in attribute '%s' is not in the same group"
		(name attr))
   :attr attr})

(deferror not-a-member-error [attr-error]
  [attr uri]
  {:msg (format "The user in attribute '%s' is not a member of the group: %s"
		(name attr) uri)
   :attr attr})

(defn check-known-attrs* [obj known-attrs]
  (doseq [attr (keys obj)
	  :when (not (known-attrs attr))]
    (raise unknown-attr-error attr))
  obj)

(defn check-read-only-attrs* [obj read-only-attrs]
  (doseq [attr (keys obj)
	  :when (read-only-attrs attr)]
    (raise read-only-attr-error attr))
  obj)

(defn check-required-attrs* [obj required-attrs]
  (doseq [attr required-attrs
	  :when (not (contains? obj attr))]
    (raise missing-attr-error attr))
  obj)

(defn check-attr-types* [obj attr-validators attr-type-names]
  (doseq [[attr val] obj]
    (when-not ((get attr-validators attr (constantly true)) val)
      (raise type-error attr (attr-type-names attr) (type val))))
  obj)