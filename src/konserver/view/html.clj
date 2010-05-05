(ns konserver.view.html
  (:require [compojure.html :as html]
	    (konserver 
	     [date :as date]
	     [resource :as res]
	     [format :as fmt]
	     [res-id :as id]
	     [uri :as uri]
	     [model :as model])
	    [konserver.view.common :as view])
  (:use [konserver.util :only (name-or-string md5)]))

(defn html-doc [title & body]
  (html/html
   (html/doctype :html5)
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:title title]
     (html/include-css "/static/style.css")
     (html/include-js "/static/jquery.js" "/static/script.js")]
    [:body
     [:h1 title]
     body]]))

(def labels
     {::res/root "Konserver"
      ::res/user-coll "Users"
      ::res/user :user
      ::res/group-coll "Groups"
      ::res/group :group-name
      ::res/member-coll "Members"
      ::res/member :member
      ::res/debt-coll "Debts"
      ::res/debt :debt
      ::res/debt-rev-coll "Revisions"
      ::res/debt-rev :revision})

(defn label [res-id]
  (html/escape-html
   (let [l (labels (:type res-id))
	 l (if (string? l)
	     l
	     (name-or-string (l res-id)))]
     l)))

(defn link-to-res [res-id]
  (html/link-to (uri/for res-id) (label res-id)))

(defn breadcrumb [res-id]
  ;; U+2192 RIGHTWARDS ARROW
  [:p (interpose " → " (map link-to-res (id/path res-id)))])

(defn with-sign [x]
  (cond (pos? x) (str "+" x)     ;; U+002B PLUS SIGN
	(neg? x) (str "−" (- x)) ;; U+2212 MINUS SIGN
	(zero? x) "±0"))         ;; U+00B1 PLUS-MINUS SIGN

(defn yes-no [x]
  (if x "yes" "no"))

(defmethod view/render-body [::res/error ::fmt/html]
  [error _]
  (html-doc
   (format "Error %d: %s" (:code (meta error)) (:name (meta error)))
   [:pre (pr-str error)]))

(defmethod view/render-body [::res/created ::fmt/html]
  [{uri :uri} _]
  (html-doc
   "Created"
   [:p
    "The resource as successfully been added and has the following URI: "
    (html/link-to uri uri)]))

(defmethod view/render-body [::res/deleted ::fmt/html]
  [{uri :collection} _]
  (html-doc
   "Deleted"
   [:p
    "The resource has been deleted from the collection: "
    (html/link-to uri uri)]))

(defmethod view/render-body [::res/root ::fmt/html]
  [root _]
  (html-doc
   (:name root)
   (html/unordered-list
    [(html/link-to (:users root)
		   "Users on this server")
     (html/link-to (:groups root)
		   "Groups on this server")])
   [:h2 "See Also"]
   [:p (html/link-to "http://www.raek.se/doc/konserver/"
		     "Konserver Documentation")]))

(def gravatar-format
     "http://www.gravatar.com/avatar/%s?s=%d&d=wavatar")

(defn- avatar
  ([email-md5]
     (avatar email-md5 80))
  ([email-md5 size]
     [:img.avatar {:alt "Avatar"
		   :src (format gravatar-format email-md5 size)}]))

(defmethod view/render-body [::res/user-coll ::fmt/html]
  [user-coll _]
  (html-doc
   "Users"
   (breadcrumb (:res-id (meta user-coll)))
   (if (empty? (:users user-coll))
     [:p [:i "There are no users."]]
     [:table
      [:tr [:th] [:th "Name"]]
      (for [user (:users user-coll)]
	[:tr
	 [:td (when (:email-md5 user)
		(avatar (:email-md5 user) 30))]
	 [:td (html/link-to (:uri user) (:name user))]])])
   [:div#show-panel
    [:p.buttons [:a#edit-link {:href "#"} "Add new user"]]]
   [:div#edit-panel
    (html/form-to
     [:post (uri/for (:res-id (meta user-coll)))]
     [:table.vertical
      [:tr
       [:th (html/label :nick "User ID: *")]
       [:td (html/text-field :nick)]]
      [:tr
       [:th (html/label :password "Password: *")]
       [:td (html/password-field :password)]]
      [:tr
       [:th (html/label :name "Name: *")]
       [:td (html/text-field :name)]]
      [:tr
       [:th (html/label :email "Email:")]
       [:td (html/text-field :email)]]]
     [:p.buttons
      [:a#cancel-link {:href "#"} "Cancel"] " "
      (html/submit-button "Add")])]))

(defmethod view/render-body [::res/user ::fmt/html]
  [user _]
  (html-doc
   (str "User: " (:name user))
   (breadcrumb (:res-id (meta user)))
   (when (:deleted user)
     [:p [:i "This user account is to be considered deleted. "
	  "The information below is stored for historical purpose."]])
    (when (:email-md5 user)
      [:p (avatar (:email-md5 user))])
   [:div#show-panel
    [:table.vertical
     [:tr
      [:th "ID:"]
      [:td (:nick user)]]
     [:tr
      [:th "Name:"]
      [:td (:name user)]]
     [:tr
      [:th "Email:"]
      [:td (or [:span.enum "(hidden)"]
	       [:span.enum "(no value)"])]]]
    [:p.buttons [:a#edit-link {:href "#"} "Edit"]]]
   [:div#edit-panel
    (html/form-to
     [:put (uri/for (:res-id (meta user)))]
     [:table.vertical
      [:tr
       [:th (html/label :nick "ID:")]
       [:td (:nick user)]]
      [:tr
       [:th (html/label :name "Name:")]
       [:td (html/text-field :name (:name user))]]
      [:tr
       [:th (html/label :email "Email:")]
       [:td (html/text-field :email)]]]
     [:p.buttons
      [:a#cancel-link {:href "#"} "Cancel"] " "
      (html/submit-button "Save")])]))

(defmethod view/render-body [::res/group-coll ::fmt/html]
  [group-coll _]
  (html-doc
   "Groups"
   (breadcrumb (:res-id (meta group-coll)))
   (if (empty? (:groups group-coll))
     [:p [:i "There are no groups."]]
     (html/ordered-list
      (for [group (:groups group-coll)]
	(html/link-to (:uri group) (:name group)))))))

(defmethod view/render-body [::res/group ::fmt/html]
  [group _]
  (html-doc
   (str "Group: " (:name group))
   (breadcrumb (:res-id (meta group)))
   (html/unordered-list
    [(html/link-to (:members group)
		   "Members of this group")
     (html/link-to (:debts group)
		   "Debts within this group")])))

(defmethod view/render-body [::res/member-coll ::fmt/html]
  [member-coll _]
  (html-doc
   (str "Members of " (-> member-coll meta :res-id :group-name)) 
   [:script {:type "text/javascript"}
    (format "var user_coll_uri = \"%s\"; var member_coll = %s;"
	    (uri/for (id/user-coll (:res-id (meta member-coll))))
	    (view/render-body member-coll ::fmt/json))]
   (html/include-js "/static/member-coll.js")
   (breadcrumb (:res-id (meta member-coll)))
   (when (:updated member-coll)
     [:p [:i "Balances last updated: "
	  (date/human-format-date (:updated member-coll))]])
   (if (empty? (:members member-coll))
     [:p [:i "There are no members."]]
     [:table.horizontal
      [:tr [:th] [:th "Member"] [:th {:colspan 3} "Balance"]]
      (for [{:keys [uri name current-balance email-md5]}
	    (reverse (sort-by :current-balance (:members member-coll)))]
	[:tr
	 [:td (when email-md5
		(avatar email-md5 30))]
	 [:td (html/link-to uri name)]
	 [:td.num (with-sign current-balance)]
	 [:td.neg [:img {:src "/static/red.png"
			 :height 10
			 :width (#(if (neg? %) (- %) 0) (int (/ current-balance 4)))}]]
	 [:td.pos [:img {:src "/static/green.png"
			 :height 10
			 :width (#(if (pos? %) % 0) (int (/ current-balance 4)))}]]])])
   [:div#show-panel
    [:p.buttons [:a#edit-link {:href "#"} "Add new member"]]]
   [:div#loading {:style "display: none"}
    [:img#spinner {:src "/static/spinner.gif" :alt ""}]
    "Loading users..."]
   [:div#edit-panel
    (html/form-to
     [:post (uri/for (:res-id (meta member-coll)))]
     [:table.vertical
      [:tr
       [:th (html/label :user "User:")]
       [:td (html/drop-down :user [["(select a user)" ""]])]]]
     [:p.buttons
      [:a#cancel-link {:href "#"} "Cancel"] " "
      (html/submit-button "Add")])]))

(defmethod view/render-body [::res/member ::fmt/html]
  [{user-uri :user
    name :name
    balance :current-balance
    updated :updated
    :as member}
   _]
  (html-doc
   (str "Member: " name)
   (breadcrumb (:res-id (meta member)))
   [:p [:i "Balance last updated: "
	(date/human-format-date updated)]]
   [:div#show-panel
    [:table.vertical
     [:tr
      [:th "User:"]
      [:td (html/link-to user-uri name)]
     [:tr
      [:th "Balance:"]
      [:td (with-sign balance)]]]]
    [:p.buttons [:a#edit-link {:href "#"} "Edit"]]]
   [:div#edit-panel
    (html/form-to
     [:delete (uri/for (:res-id (meta member)))]
     [:table.vertical
      [:tr
       [:th (html/label :nick "User:")]
       [:td name]]
      [:tr
       [:th (html/label :name "Balance:")]
       [:td (with-sign balance)]]]
     [:p.buttons
      [:a#cancel-link {:href "#"} "Cancel"] " "
      (html/submit-button "Delete...")])]))

(defmethod view/render-body [::res/debt-coll ::fmt/html]
  [debt-coll _]
  (html-doc
   (str "Debts within " (-> debt-coll meta :res-id :group-name))
   [:script {:type "text/javascript"}
    (format "var user_coll_uri = \"%s\"; var member_coll_uri = \"%s\";var debt_coll = %s;"
	    (uri/for (id/user-coll (:res-id (meta debt-coll))))
	    (uri/for (id/member-coll (:res-id (meta debt-coll))))
	    (view/render-body debt-coll ::fmt/json))]
   (html/include-js "/static/debt-coll.js")
   (breadcrumb (:res-id (meta debt-coll)))
   [:div#loading
    [:img#spinner {:src "/static/spinner.gif" :alt ""}]
    "Loading..."]
   [:table#debts {:class "horizontal"}
    [:tr [:th "Name"] [:th "Amt"] [:th "Creditor"] [:th "Debtors"]]]
   [:div#show-panel
    [:p.buttons [:a#edit-link {:href "#"} "Add new debt"]]]
   [:div#edit-panel
    (html/form-to
     [:post (uri/for (:res-id (meta debt-coll)))]
     (html/hidden-field :comment "initial")
     [:table.vertical
      [:tr
       [:th (html/label :name "Title: *")]
       [:td (html/text-field :name)]]
      [:tr
       [:th (html/label :amount "Amount: *")]
       [:td (html/text-field :amount)]]
      [:tr
       [:th (html/label :creditor "Creditor: *")]
       [:td (html/drop-down :creditor [["(select a member)" ""]])]]
      [:tr
       [:th (html/label :amount "Debtors: *")]
       [:td#debtors]]
      [:tr
       [:th (html/label :description "Description:")]
       [:td (html/text-area :description)]]]
     [:p.buttons
      [:a#cancel-link {:href "#"} "Cancel"] " "
      (html/submit-button "Add")])]))

(defmethod view/render-body [::res/debt ::fmt/html]
  [{:keys [current-revision updated editor comment
	   name amount creditor debtors description revisions
	   to-creditor from-debtor], :as debt} _]
  (html-doc
   (str "Debt: " (html/escape-html name))
   [:script {:type "text/javascript"}
    (format "var user_coll_uri = \"%s\"; var member_coll_uri = \"%s\"; var debt = %s;"
	    (uri/for (id/user-coll (:res-id (meta debt))))
	    (uri/for (id/member-coll (:res-id (meta debt))))
	    (view/render-body debt ::fmt/json))]
   (html/include-js "/static/debt.js")
   (breadcrumb (:res-id (meta debt)))
   [:div#loading
    [:img#spinner {:src "/static/spinner.gif" :alt ""}]
    "Loading..."]
   [:p [:i
	"Latest edit by " [:span#editor (html/link-to editor editor)]
	" at " (date/human-format-date updated)
	": \"" [:tt (html/link-to current-revision (html/escape-html comment)) ]
	"\" (" (html/link-to revisions "History") ")"]]
   [:div#show-panel
    [:table.vertical
     [:tr
      [:th "Title:"]
      [:td (html/escape-html name)]]
     [:tr
      [:th "Amount:"]
      [:td amount]]
     [:tr
      [:th "– to Creditor:"]
      [:td to-creditor]]
     [:tr
      [:th "– per Debtor:"]
      [:td from-debtor]]
     [:tr
      [:th "Creditor:"]
      [:td#show-creditor (html/link-to creditor creditor)]]
     [:tr
      [:th "Debtors:"]
      [:td#show-debtors (interpose ", " (map #(html/link-to % %) debtors))]]
     [:tr
      [:th "Description:"]
      [:td (if description
	     (html/escape-html description)
	     [:span.enum "(no value)"])]]
     ]
    [:p.buttons [:a#edit-link {:href "#"} "Edit"]]]
   [:div#edit-panel
    (html/form-to
     [:put (uri/for (:res-id (meta debt)))]
     (html/hidden-field :current-revision current-revision)
     [:table.vertical
      [:tr
       [:th (html/label :name "Title:")]
       [:td (html/text-field :name (html/escape-html name))]]
      [:tr
       [:th (html/label :amount "Amount:")]
       [:td (html/text-field :amount amount)]]
      [:tr
       [:th (html/label :creditor "Creditor:")]
       [:td (html/drop-down :creditor [["(select a member)" ""]])]]
      [:tr
       [:th (html/label :amount "Debtors:")]
       [:td#debtors]]
      [:tr
       [:th (html/label :description "Description:")]
       [:td (html/text-area :description (html/escape-html description))]]
      [:tr
       [:th (html/label :comment "Edit Comment: *")]
       [:td (html/text-field :comment)]]]
     [:p.buttons
      [:a#cancel-link {:href "#"} "Cancel"] " "
      (html/submit-button "Save")])]))
