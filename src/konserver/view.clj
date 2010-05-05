(ns konserver.view
  (:require [konserver.view common json clj html xml form])
  (:use [clojure.contrib.ns-utils :only [immigrate]]))

(immigrate 'konserver.view.common)
