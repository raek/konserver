(ns konserver.main
  (:require [swank.swank :as swank]
	    [compojure.server.jetty :as jetty]
	    [konserver.uri :as uri])
  (:use (clojure.contrib pprint except)
	(compojure.http servlet routes helpers)
	[konserver.controller :only [handle-ring-request]]
	[konserver.util :only [cond-re-let]])
  (:import java.io.File)
  (:gen-class))

(def konserver-version "0.1.2")

(defroutes konserver-routes
  (ANY "/request"
       {:status 200
        :headers {"Content-Type" "text/plain; charset=utf-8"}
        :body (with-out-str (pprint request))})
  (GET "/reset-auth"
       {:status 401
	:headers {"WWW-Authenticate" "Basic realm=\"Konserver\""}
	:body "Use this page to force your browser to ask for credentials"})
  (GET "/static/*"
       (serve-file "static" (get-in request [:route-params :*])))
  (ANY "*"
       (handle-ring-request request)))

(def http-server (ref {:running false, :server nil}))

(defn start-swank-server [port]
  (clojure.main/with-bindings
    (swank/ignore-protocol-version "2009-03-09")
    (swank/start-server ".slime-socket"
			:port port
			:encoding "utf-8"
			:dont-close true)))

(defn uri-root [hostname port]
  (if (= port 80)
    (format "http://%s/" hostname)
    (format "http://%s:%d/" hostname port)))

(defn start-http-server [hostname port]
  (dosync
   (assert (not (:running @http-server)))
   (let [server (jetty/jetty-server {:port port}
				    "/*" (servlet konserver-routes))]
     (jetty/start server)
     (ref-set uri/*root* (uri-root hostname port))
     (alter http-server assoc
	    :running true
	    :server server))))

(defn stop-http-server []
  (dosync
   (assert (:running @http-server))
   (jetty/stop (:server @http-server))
   (alter http-server assoc
	  :running false
	  :server nil)))

(defn parse-option [s]
  (cond-re-let s
	       #"^--([^=]+)=(.*)" [flag value] [(keyword flag) value]
	       #"^--(.*)" [flag] [(keyword flag) nil]
	       (throw-arg "Illegal option syntax: \"%s\"" s)))

(defn parse-options [string-seq]
  (into {} (map parse-option string-seq)))

(def known-flags
     #{:help :load-data :data-dir :repl
       :http :http-port :http-hostname
       :swank :swank-port})

(defn check-options [opts]
  (doseq [[flag _] opts]
    (when-not (known-flags flag)
      (throw-arg "Unknown option: \"%s\"" (name flag))))
  opts)

(def default-data-dir "data/")
(def default-http-port "8080")
(def default-http-hostname "localhost")
(def default-swank-port "4005")

(defn startup [opts]
  (let [data-dir (File. (get opts :data-dir default-data-dir))
	http-port (Integer/parseInt
		   (get opts :http-port default-http-port))
	http-hostname (get opts :http-hostname default-http-hostname)
	swank-port (Integer/parseInt
		    (get opts :swank-port default-swank-port))]
    (println (format "Starting Konserver %s" konserver-version))
    (when (or (contains? opts :load-data)
	      (contains? opts :http))
      (println (format "Loading data from direcory \"%s\"" data-dir)))
    (when (contains? opts :http)
      (println (format "Starting HTTP server with hostname \"%s\" on port %d"
		       http-hostname http-port))
      (start-http-server http-hostname http-port))
    (when (contains? opts :swank)
      (println (format "Starting Swank server in port %d"
		       swank-port))
      (start-swank-server swank-port))
    (when (contains? opts :repl)
      (println "Starting Clojure REPL")
      (future (clojure.main/repl :init #(in-ns 'user))
	      (newline)
	      (flush)
	      (System/exit 0)))))

(defn shutdown []
  (println (format "Shutting down Konserver")))

(defn -main [& args]
  (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown))
  (startup (check-options (parse-options args))))

