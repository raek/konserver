(ns konserver.format
  "Supported data exchange formats: MIME types and file extensions."
  (:require [clojure.set :as set]))

(def supported
     #{{:id ::html, :extension "html", :mime-type "text/html"}
       {:id ::xml,  :extension "xml",  :mime-type "application/xml"}
       {:id ::json, :extension "json", :mime-type "application/json"}
       {:id ::clj,  :extension "clj",  :mime-type "text/x-clojure-source"}
       {:id ::form, :extension "form", :mime-type "application/x-www-form-urlencoded"}})

(doseq [format [::html ::json ::clj]]
  (derive format ::any))

(def default ::html)

(defn to-extension [id]
  (-> ((set/index supported [:id]) {:id id})
      first
      :extension))

(defn to-mime-type [id]
  (-> ((set/index supported [:id]) {:id id})
      first
      :mime-type))

(defn by-mime-type [mime-type]
  {:pre ([(string? mime-type)])}
  (let [[_ mt _] (re-find #"^([^;]*)" mime-type)]
    (-> ((set/index supported [:mime-type]) {:mime-type mt})
	first
	:id)))

(defn by-extension [extension]
  (-> ((set/index supported [:extension]) {:extension extension})
      first
      :id))
