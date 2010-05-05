(ns konserver.view.xml
  (:require (konserver [resource :as res]
		       [format :as fmt])
	    [konserver.view.common :as view])
  (:use (clojure.contrib prxml
			 [str-utils2 :only [capitalize]])))

(defn xml-name [kw]
  (let [name-words (re-seq #"[^-]+" (name kw))]
    (apply str (first name-words) (map capitalize (rest name-words)))))

(defn singular [kw]
  {:pre [(.endsWith (name kw) "s")]}
  (keyword (.substring (name kw) 0 (dec (.length (name kw))))))

(defn xml-tree [type data]
  (cond (map? data) (into [(xml-name type)]
			  (map xml-tree (keys data) (vals data)))
	(coll? data) (into [(xml-name type)]
			   (map #(xml-tree (singular type) %) data))
	(keyword? data) [(xml-name type) (xml-name data)]
	:else [(xml-name type) (str data)]))

(defmethod view/render-body [::res/any ::fmt/xml]
  [data _]
  {:pre [(map? data)]
   :post [(string? %)]}
  (with-out-str
    (binding [*prxml-indent* 2]
      (prxml (xml-tree (type data) data)))))