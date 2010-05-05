(ns konserver.controller.test.uri
  (:require (konserver [resource :as res]
		       [uri :as uri]))
  (:use clojure.test
	[konserver.config :only [*root*]]))

(deftest test-parse
  (binding [*root* "http://konserver.example.com/"]
    
    (is (= (uri/parse (str *root* ""))
	   {:type ::res/root})
	"server root resource")
    
    (is (= (uri/parse (str *root* "users"))
	   {:type ::res/user-coll})
	"user collection")
    
    (is (= (uri/parse (str *root* "users/achilles"))
	   {:type ::res/user
	    :user :achilles})
	"user entity")
    
    (is (= (uri/parse (str *root* "groups"))
	   {:type ::res/group-coll})
	"group collection")
    
    (is (= (uri/parse (str *root* "groups/demo"))
	   {:type ::res/group
	    :group :demo})
	"group entity")
    
    (is (= (uri/parse (str *root* "groups/demo/members"))
	   {:type ::res/member-coll
	    :group :demo})
	"member collection")
    
    (is (= (uri/parse (str *root* "groups/demo/members/achilles"))
	   {:type ::res/member
	    :group :demo
	    :member :achilles})
	"member entity")
    
    (is (= (uri/parse (str *root* "groups/demo/debts"))
	   {:type ::res/debt-coll
	    :group :demo})
	"debt collection")
    
    (is (= (uri/parse (str *root* "groups/demo/debts/123"))
	   {:type ::res/debt
	    :group :demo
	    :debt 123})
	"debt entity")
    
    (is (= (uri/parse (str *root* "groups/demo/debts/123/revisions"))
	   {:type ::res/debt-rev-coll
	    :group :demo
	    :debt 123})
	"revision collection")
    
    (is (= (uri/parse (str *root* "groups/demo/debts/123/revisions/3"))
	   {:type ::res/debt-rev
	    :group :demo
	    :debt 123
	    :revision 3})
	"revision entity")))

(deftest test-for
  (binding [*root* "http://konserver.example.com/"]
    
    (is (= (uri/for {:type ::res/root})
	   (str *root* ""))
	"server root resource")
    
    (is (= (uri/for {:type ::res/user-coll})
	   (str *root* "users"))
	"user collection")
    
    (is (= (uri/for {:type ::res/user
		     :user :achilles})
	   (str *root* "users/achilles"))
	"user entity")
    
    (is (= (uri/for {:type ::res/group-coll})
	   (str *root* "groups"))
	"group collection")
    
    (is (= (uri/for {:type ::res/group
		     :group :demo})
	   (str *root* "groups/demo"))
	"group entity")
    
    (is (= (uri/for {:type ::res/member-coll
		     :group :demo})
	   (str *root* "groups/demo/members"))
	"member collection")
    
    (is (= (uri/for {:type ::res/member
		     :group :demo
		     :member :achilles})
	   (str *root* "groups/demo/members/achilles"))
	"member entity")
    
    (is (= (uri/for {:type ::res/debt-coll
		     :group :demo})
	   (str *root* "groups/demo/debts"))
	"debt collection")
    
    (is (= (uri/for {:type ::res/debt
		     :group :demo
		     :debt 123})
	   (str *root* "groups/demo/debts/123"))
	"debt entity")
    
    (is (= (uri/for {:type ::res/debt-rev-coll
		     :group :demo
		     :debt 123})
	   (str *root* "groups/demo/debts/123/revisions"))
	"revision collection")
    
    (is (= (uri/for {:type ::res/debt-rev
		     :group :demo
		     :debt 123
		     :revision 3})
	   (str *root* "groups/demo/debts/123/revisions/3"))
	"revision entity")))
