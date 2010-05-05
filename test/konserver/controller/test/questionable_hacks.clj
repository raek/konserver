(ns konserver.controller.test.questionable-hacks
  (:use clojure.test
	konserver.controller.questionable-hacks))

(deftest test-with-fake-put-and-delete
  (let [handler (with-fake-put-and-delete identity)]

    (testing "Changes the method when given _method=PUT form parameter"
      (let [request {:request-method :post
		     :form-params {:_method "PUT"}}
	    response (handler request)]
	(is (= :put (:request-method response)))))

    (testing "Changes the method when given _method=DELETE form parameter"
      (let [request {:request-method :post
		     :form-params {:_method "DELETE"}}
	    response (handler request)]
	(is (= :delete (:request-method response)))))))
