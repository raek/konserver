(ns konserver.controller.questionable-hacks
  (:require [konserver.format :as format])
  (:use [clojure.contrib.core :only [dissoc-in]]))

(defn with-fake-put-and-delete [handler]
  (fn [{method :request-method, params :form-params, :as request}]
    (if (and (= method :post)
	     (contains? params :_method))
      (let [new-method (get {"PUT"    :put
			     "DELETE" :delete}
			    (.toUpperCase (:_method params))
			    :post)]
	(handler (-> request
		     (dissoc-in [:form-params :_method])
		     (dissoc-in [:params :_method])
		     (assoc :request-method new-method))))
      (handler request))))

(defn with-priotitized-html [handler]
  "WebKit based user agents have a weird habit of desiring any type of XML
   over HTML. This middleware changes the Accept header to only text/html
   if the user agent is WebKit based and requests HTML, potentialy among
   other formats."
  (fn [request]
    (let [accept     (get-in request [:headers "accept"])
	  user-agent (get-in request [:headers "user-agent"])]
      (if (and (not= (.indexOf accept "text/html") -1)
	       (not= (.indexOf user-agent "WebKit") -1))
	(handler (assoc-in request [:headers "accept"] "text/html"))
	(handler request)))))

(defn- split-uri-extension
  "Splits a URI into the URI without the file extension and the extension.
   Returns nil if the URI doesn't include an file extension.
   
   (split-uri-file-extension \"/foo.xml\")
   => [\"/foo\" \"xml\"]"
  [uri]
  (when-let [[_ base extension] (re-find #"(.*)\.([A-Za-z0-9_]+)" uri)]
    [base extension]))

(defn with-file-extension
  "Middleware decorator function for stripping file extensions from request
   URIs and filling in the Accept header with the corresponding MIME type. If
   the extension is not recognized, neither the URI nor the Accept header is
   touched. The extension-to-mime-type is used for looking up the mime-type
   for the file extension and should return nil for unrecognized extensions."
  [handler]
  (fn [request]
    (if-let [[new-uri extension] (split-uri-extension (:uri request))]
      (if-let [mime-type (-> extension
			     format/by-extension
			     format/to-mime-type)]
	(handler (-> request
		     (assoc-in [:uri] new-uri)
		     (assoc-in [:headers "accept"] mime-type)))
	(handler request))
      (handler request))))

