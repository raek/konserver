(ns konserver.view.common
  (:require konserver.resource
	    [konserver.format :as format]))

(defmulti parse-body
  "Parses a data object serialized in the given format. Returns a map."
  {:arglists '([stream type format])}
  (fn [stream type format] [type format]))

(defmulti parse-form
  "Parses a data object from a WWW form. Returns a map."
  {:arglists '([form-map type])}
  (fn [form-map type] type))

(defmulti render-body
  "Renders a data object in the given format. Returns a string."
  {:arglists '([obj format])}
  (fn [obj format] [(type obj) format]))

(defn render
  "Renders a data object in the given format. Returns a Ring response."
  ([obj format]
     (let [code (get (meta obj) :code 200)
	   mime-type (format/to-mime-type format)
	   headers (:headers (meta obj))]
     {:status code
      :headers (merge {"Content-Type" mime-type} headers)
      :body (when-not (= code 204)
	      (render-body obj format))})))
