(ns konserver.controller.accept
  "Middleware for content negotiation with the Accept header."
  (:require [konserver.format :as format]))

(defn parse-media-range [string]
  (let [[mime-type & param-strings] (map #(.trim %)
					 (re-seq #"[^;]+" string))
	[_ type subtype]            (re-find #"([^/]+)/(.*)"
					     mime-type)
	params (into {}             (map (fn [[_ k v]]
					   [(keyword k) v])
					 (map #(re-find #"([^=]+)=(.*)" %)
					      param-strings)))]
    {:mime-type mime-type
     :type type
     :subtype subtype
     :q (Double/parseDouble (get params :q "1.0"))
     :params (dissoc params :q)}))

(defn compare-media-ranges [x y]
  (cond (not= (:q x) (:q y))
	(- (compare (:q x) (:q y)))
	
	(and (=    (:type x) "*")
	     (not= (:type y) "*"))
	1
	
	(and (not= (:type x) "*")
	     (=    (:type y) "*"))
	-1
	
	(and (=    (:subtype x) "*")
	     (not= (:subtype y) "*"))
	1
	
	(and (not= (:subtype x) "*")
	     (=    (:subtype y) "*"))
	-1
	
	(not= (count (:params x)) (count (:params y)))
	(- (compare (count (:params x)) (count (:params y))))

	:else
	0))

(defn matches? [content-type media-range]
  (cond (= "*" (:type media-range)) true
	(not (= (:type content-type) (:type media-range))) false
	(= "*" (:subtype media-range)) true
	(not (= (:subtype content-type) (:subtype media-range))) false
	:else true))

(defn parse-accept-header [string]
  (let [media-ranges (->> string
			  (re-seq #"[^,]+")
			  (map parse-media-range)
			  (sort compare-media-ranges))]
    media-ranges))

(defn first-supported [media-ranges]
  (let [supported-mime-types (->> format/supported
				  (map :mime-type)
				  (map parse-media-range))
	step (fn [media-ranges]
	       (when-first [media-range media-ranges]
		 (or (first (filter #(matches? % media-range)
				    supported-mime-types))
		     (recur (rest media-ranges)))))]
    (step media-ranges)))

(defn with-accept [handler]
  (fn [request]
    (let [accept-header (get (:headers request)
			     "accept"
			     (format/to-mime-type format/default))
	  media-ranges (parse-accept-header accept-header)
	  mime-type (:mime-type (first-supported media-ranges))]
      (if mime-type
	(handler (assoc request
		   :konserver.controller/format (format/by-mime-type mime-type)))
	{:status 406
	 :headers {"Content-Type" "text/plain"}
	 :body (with-out-str
		 (println "responses:")
		 (doseq [format format/supported]
		   (println (:mime-type format)))
		 (newline)
		 (println "requests:")
		 (println "application/x-www-form-urlencoded"))}))))

