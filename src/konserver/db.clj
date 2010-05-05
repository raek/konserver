(ns konserver.db
  (:require [clojure.set :as set]))

(defn union
  ([]
     #{})
  ([set]
     set)
  ([set1 & sets]
     (into set1 (apply concat sets))))

(defn make-table
  "Creates a new, empty table."
  []
  #{})

(defn insert
  "Returns a new table with the record in it."
  [db rec]
  (conj db rec))

(defn select
  "Returns a subset of the table, matching a criterion."
  [db search-map]
  (let [idx (set/index db (keys search-map))]
    (get idx search-map)))

(defn update
  "Returns a new table with the update applied."
  [db search-map update-map]
  (let [idx (set/index db (keys search-map))]
    (apply conj
	   (apply union (vals (dissoc idx search-map)))
	   (map #(merge % update-map) (get idx search-map)))))

(defn delete
  "Returns a new table without the record in it."
  [db search-map]
  (let [idx (set/index db (keys search-map))]
    (apply union (vals (dissoc idx search-map)))))
