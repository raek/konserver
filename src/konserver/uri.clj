(ns konserver.uri
  (:refer-clojure :exclude [for])
  (:require [konserver.resource :as res]))

(declare parse-root parse-users parse-groups
	 parse-members parse-debts parse-revisions
	 parse-debug)

(def *root* (ref nil))

(defn with-root [uri]
  (str (.substring @*root* 0 (dec (.length @*root*))) uri))

(defn- parse-int [s]
  (binding [*read-eval* false]
    (read-string s)))

(defn parse [uri]
  (when (.startsWith uri @*root*)
    (let [path-string (.substring uri (.length @*root*))
	  path (re-seq #"[^/]+" path-string)]
      (parse-root path))))
  
(defn- parse-root [path]
  (condp = (first path)
    nil {:type ::res/root}
    "users" (parse-users (rest path))
    "groups" (parse-groups (rest path))
    "debug" {:type ::res/debug}
    "debugs" {:type ::res/debug-coll}
    nil))

(defn- parse-users [path]
  (condp = (count path)
    0 {:type ::res/user-coll}
    1 {:type ::res/user,
       :user (keyword (first path))}
    nil))

(defn- parse-groups [path]
  (if (empty? path)
    {:type ::res/group-coll}
    (let [group (keyword (first path))]
      (condp = (second path)
	nil {:type ::res/group,
	     :group (keyword (first path))}
	"members" (parse-members group (rest (rest path)))
	"debts" (parse-debts group (rest (rest path)))
	nil))))

(defn- parse-members [group path]
  (condp = (count path)
    0 {:type ::res/member-coll
       :group group}
    1 {:type ::res/member
       :group group
       :member (keyword (first path))}
    nil))

(defn- parse-debts [group path]
  (if (empty? path)
    {:type ::res/debt-coll
     :group group}
    (let [debt (parse-int (first path))]
      (condp = (second path)
	nil {:type ::res/debt
	     :group group
	     :debt (parse-int (first path))}
	"revisions" (parse-revisions group debt (rest (rest path)))))))

(defn- parse-revisions [group debt path]
  (condp = (count path)
    0 {:type ::res/debt-rev-coll
       :group group
       :debt debt}
    1 {:type ::res/debt-rev
       :group group
       :debt debt
       :revision (parse-int (first path))}
    nil))

(defn- build-uri [path]
  (let [root (.substring @*root* 0 (dec (.length @*root*)))]
    (apply str (interpose "/" (cons root path)))))

(defn for [res-id]
  (condp = (:type res-id)
    ::res/root
    (build-uri [""]),
    ::res/user-coll
    (build-uri ["users"]),
    ::res/user
    (build-uri ["users" (name (:user res-id))]),
    ::res/group-coll
    (build-uri ["groups"]),
    ::res/group
    (build-uri ["groups" (name (:group res-id))]),
    ::res/member-coll
    (build-uri ["groups" (name (:group res-id))
		"members"]),
    ::res/member
    (build-uri ["groups" (name (:group res-id))
		"members" (name (:member res-id))]),
    ::res/debt-coll
    (build-uri ["groups" (name (:group res-id))
		"debts"]),
    ::res/debt
    (build-uri ["groups" (name (:group res-id))
		"debts" (str (:debt res-id))]),
    ::res/debt-rev-coll
    (build-uri ["groups" (name (:group res-id))
		"debts" (str (:debt res-id))
		"revisions"]),
    ::res/debt-rev
    (build-uri ["groups" (name (:group res-id))
		"debts" (str (:debt res-id))
		"revisions" (str (:revision res-id))])
    (throw (Exception. (format "Cannot make URI for %s" (:type res-id))))))