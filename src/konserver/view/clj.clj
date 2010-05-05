(ns konserver.view.clj
  (:require [konserver
	     [resource :as res]
	     [format :as fmt]]
	    [konserver.view.common :as view])
  (:use [clojure.contrib.pprint
	 :only [pprint *print-right-margin* *print-miser-width*]])
  (:import (java.io InputStreamReader PushbackReader)))

(defmethod view/parse-body [::res/any ::fmt/clj]
  [stream type _]
  {:post [(map? %)]}
  (let [pbr (-> stream (InputStreamReader. "UTF-8") PushbackReader.)]
    (binding [*read-eval* false]
      (with-meta (read pbr) {:type type}))))

(defmethod view/render-body [::res/any ::fmt/clj]
  [data _]
  {:pre [(map? data)]
   :post [(string? %)]}
  (binding [*print-right-margin* 70]
    (with-out-str (pprint data))))
