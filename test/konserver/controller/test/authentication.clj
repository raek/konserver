(ns konserver.controller.test.authentication
  (:use clojure.test konserver.controller.authentication))

(defn- mock-auth-fn [user pass]
  (if (and (= user "Aladdin")
	   (= pass "open sesame"))
    ::ok
    ::not-ok))



(deftest test-with-http-basic-authentication
  (let [handler (with-http-basic-authentication identity  mock-auth-fn)]

    (testing "User is nil if no credentials are given"
      (let [request {:a 1, :b 2}
	    response (handler request)]
	(is (= nil (:konserver.controller/user response)))))

    (testing "User is nil if authentication scheme is not supported"
      (let [credentials "Unknown ABCabcXYZxyz"
	    request {:a 1, :b 2, :headers {"authorization" credentials}}
	    response (handler request)]
	(is (= nil (:konserver.controller/user response)))))

    (testing "User is parsed from HTTP Basic Authentication credentials"
      (let [credentials "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
	    request {:a 1, :b 2, :headers {"authorization" credentials}}
	    response (handler request)]
	(is (= ::ok (:konserver.controller/user response)))))))



(deftest test-parse-credentials
  
  (testing "Nil yields nil"
    (is (nil? (parse-credentials nil))))
  
  (testing "Returns nil on unknown authentication scheme"
    (is (nil? (parse-credentials "Unknown ABCabcXYZxyz"))))
  
  (testing "Returns nil if colon is missing from credentials"
    (is (nil? (parse-credentials "Basic bm9jb2xvbg=="))))

  (testing "Returns username and password on well-formed credentials"
    (is (= ["Aladdin" "open sesame"]
	   (parse-credentials "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==")))))
