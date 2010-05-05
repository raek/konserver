(ns konserver.controller.authentication
  "Middleware for HTTP Basic Authentication"
  (:use [konserver.util :only [b64decode]]))

(def parse-credentials)

(defn with-http-basic-authentication
  "Decorator for HTTP Basic Authentication. This middleware sets the given key
   to the value returned by auth-fn when called with the username and password
   as arguments. auth-fn should take two arguments, the username and the
   password, and return something that identifies the user, if the credentials
   are valid."
  [handler auth-fn]
  (fn [{headers :headers, :as request}]
    (if-let [[user pass] (parse-credentials (get headers "authorization"))]
      (handler (assoc request :konserver.controller/user (auth-fn user pass)))
      (handler (assoc request :konserver.controller/user nil)))))

(defn- nil-or?
  "Returns true if x is nil or (pred x) is true, else false."
  [pred x]
  (or (nil? x) (pred x)))

(defn parse-credentials
  "Extracts the user name and password from a HTTP Basic Authentication header
   value and returns them in a vector.
   
   Usage:
   (parse-credentials \"Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\")
     => [\"Aladdin\" \"open sesame\"]"
  [auth-header-value]
  {:pre [(nil-or? string? auth-header-value)]}
  (when auth-header-value
    (when-let [[_ creds-b64]
	       (re-find #"^[Bb][Aa][Ss][Ii][Cc] (.*)$" auth-header-value)]
      (let [creds-string (b64decode creds-b64 "US-ASCII")]
	(when-let [[_ user pass]
		   (re-find #"^([^:]*):(.*)$" creds-string)]
	  [user pass])))))
