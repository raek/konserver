(ns konserver.view.json
  (:require [konserver
	     [resource :as res]
	     [format :as fmt]]
	    [konserver.view.common :as view])
  (:use [clojure.contrib.json.write :only [print-json]]))

(defmethod view/render-body [::res/any ::fmt/json]
  [data _]
  (with-out-str (print-json (with-meta data {}))))
