(defproject konserver "0.1.0"
  :description "A server for tracking debts among groups of people"
  :dependencies [[org.clojure/clojure "1.1.0"]
		 [org.clojure/clojure-contrib "1.1.0"]
		 [compojure "0.3.2"]
		 [swank-clojure "1.1.0"]
		 [commons-codec/commons-codec "1.4"]]
  :dev-dependencies [[leiningen/lein-swank "1.1.0"]]
  :namespaces :all
  :main konserver.main)
