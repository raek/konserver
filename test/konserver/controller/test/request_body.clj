(ns konserver.controller.test.request-body
  (:require (konserver [resource :as res]
		       [format :as format]
		       [view :as view]))
  (:use clojure.test konserver.controller.request-body)
  (:import java.io.ByteArrayInputStream))

(defn- to-stream [s]
  (ByteArrayInputStream. (.getBytes s "UTF-8")))

(defn parse-body-stub [stream type format]
  stream)

(def format-by-mime-type-stub
     {"application/x-unexistent" ::unexistent})



(deftest test-with-request-body
  (binding [format/by-mime-type format-by-mime-type-stub
	    view/parse-body parse-body-stub]

    (testing "Does nothing when there is no request entity"
      (are [method]
	   (let [handler (with-request-body identity)
		 before {:request-method method
			 :foo "bar"}
		 after (handler before)]
	     (= before after))
	   :head :get :delete))

    (testing "Uses URL-encoded bodies parsed by Compojure"
      (let [handler (with-request-body identity)
	    before {:request-method :post
		    :content-type "application/x-www-form-urlencoded"
		    :form-params {:a 1, :b 2}}
	    after (handler before)]
	(is (= {:a 1, :b 2} (:konserver.controller/body after)))))

    (testing "Parses request entities"
      (let [handler (with-request-body identity)
	    before {:request-method :post
		    :content-type "application/x-www-form-urlencoded"
		    :form-params {:a 1, :b 2}}
	    after (handler before)]
	(is (= {:a 1, :b 2} (:konserver.controller/body after)))))))
