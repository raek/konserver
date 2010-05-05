(ns konserver.controller.request-body
  (:require (konserver [format :as format]
		       [res-id :as id]
		       [view :as view])))

(defn with-request-body [handler]
  (fn [{:as request
	:keys [body content-type]
	method :request-method
	form :form-params}]
    (if (not (#{:post :put} method))
      (handler request)
      (let [type (:type (:konserver.controller/res-id request))
	    type (if (id/collection? type)
		   (id/child-type type)
		   type)
	    format (format/by-mime-type content-type)]
	(if (= format ::format/form)
	  (handler (assoc request
		     :konserver.controller/body-format format
		     :konserver.controller/body (view/parse-form form type)))
	  (handler (assoc request
		     :konserver.controller/body-format format
		     :konserver.controller/body (view/parse-body body type format))))))))
