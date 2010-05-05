(ns konserver.controller.test.res-id
  (:use clojure.test
	konserver.controller.res-id))

(def mock-parse-uri {"http://example.com/foo" ::foo})

(deftest test-with-resource-id
  (let [handler (with-resource-id identity mock-parse-uri)]

    (testing "Adds a resource id if given a correct URI"
      (let [request {:konserver.controller/uri "http://example.com/foo"}
	    response (handler request)]
	(is (= ::foo (:konserver.controller/res-id response)))))

    (testing "Emits a 404 error if given an unknown URI"
      (let [request {:konserver.controller/uri "http://example.com/bar"}
	    response (handler request)]
	(is (= 404 (:code (meta response))))))))
