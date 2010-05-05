(ns konserver.controller.error
  (:require [konserver
	     [resource :as res]
	     [uri :as uri]]))

(defn bad-request [data]
  (with-meta
    data
    {:type ::res/bad-request
     :code 400
     :name "Bad Request"}))

(defn unauthorized []
  (with-meta
    {}
    {:type ::res/unauthorized
     :code 401
     :name "Unauthorized"
     :headers {"WWW-Authenticate" "Basic realm=\"Konserver\""}}))

(defn not-found [uri]
  (with-meta
    {:uri uri}
    {:type ::res/not-found
     :code 404
     :name "Not Found"}))

(defn method-not-allowed [method uri]
  (with-meta
    {:method method
     :uri uri}
    {:type ::res/method-not-allowed
     :code 405
     :name "Method Not Allowed"}))

(defn not-acceptable [acceptable]
  (with-meta
    {:acceptable acceptable}
    {:type ::res/not-acceptable
     :code 406
     :name "Not Acceptable"}))

(defn conflict [data]
  (with-meta
    data
    {:type ::res/conflict
     :code 409
     :name "Conflict"}))

(defn gone [res-id]
  (with-meta
    {:uri (uri/for res-id)}
    {:type ::res/gone
     :code 410
     :name "Gone"}))

(defn internal-server-error [msg]
  (with-meta
    {:msg msg}
    {:type ::res/internal-server-error
     :code 501
     :name "Internal Server Error"}))

(defn not-implemented [msg]
  (with-meta
    {:msg msg}
    {:type ::res/not-implemented
     :code 501
     :name "Not Implemented"}))
