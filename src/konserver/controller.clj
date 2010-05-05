(ns konserver.controller
  (:require (konserver [uri :as uri]
		       [model :as model]
		       [view :as view]
		       [res-id :as id])
	    (konserver.controller [error :as error]
				  [success :as success]))
  (:use clojure.contrib.error-kit
	konserver.model.error
	(konserver.controller accept authentication request-body res-id
			      questionable-hacks)))

(declare perform-action method-to-action requires-authentication?
	 handle-konserver-request handle-ring-request
	 handle-ring-request-with-format with-full-uri with-action)

(def handle-ring-request
     (-> (fn [request]
	   {:pre  [(map? request)]
	    :post [(map? %)
		   (contains? % :status)
		   (contains? % :headers)
		   (contains? % :body)]}
	   (view/render (handle-ring-request-with-format request)
			(::format request)))
	 (with-accept)
	 (with-file-extension)
	 (with-priotitized-html)))

(defn with-full-uri [handler]
  (fn [request]
    (handler (assoc request ::uri (uri/with-root (:uri request))))))

(defn with-action [handler]
  (fn [{uri ::uri, res-id ::res-id, method :request-method, :as request}]
    (if-let [action (method-to-action method (:type res-id))]
      (handler (assoc request ::action action))
      (error/method-not-allowed method uri))))

(def handle-ring-request-with-format
     (-> (fn [request]
	   (handle-konserver-request request))
	 (with-request-body)
	 (with-http-basic-authentication model/authenticate-user)
	 (with-action)
	 (with-fake-put-and-delete)
	 (with-resource-id uri/parse)
	 (with-full-uri)))

(defn method-to-action [method type]
  (cond (id/collection? type) ({:head :peek
				:get :list
				:post :add} method)
	(id/entity? type)     ({:head :peek
				:get :retrieve
				:put :update
				:delete :delete} method)
	:else                 nil))

(defn handle-konserver-request
  [{uri ::uri, res-id, ::res-id, action ::action,
    body ::body, user ::user, format ::format, :as request}]
  {:pre  [(map? request)]
   :post [(map? %)]}
  (try
    (with-handler
      (let [db (model/select-db res-id)
	    res-id (assoc res-id :group-name (:group-name db))]
	(cond (and (requires-authentication? res-id)
		   (nil? user))
	      ;; The meaning of this error is a bit overloaded. Here it means
	      ;; that the user has to state it's identity.
	      (error/unauthorized)
	      (or (= (:type res-id) :konserver.resource/debug)
		  (= (:type res-id) :konserver.resource/debug-coll))
	      (with-meta
		{:uri uri, :res-id res-id, :action action, :body body,
		 :user user, :format format, :body-format (::body-format request)}
		{:type :konserver.resource/debug})
	      :else (perform-action action res-id db body user)))
      (handle does-not-exist-error {:as e}
	      (error/not-found uri))
      (handle already-exists-error {:keys [tag msg id]}
	      (error/conflict {:type (keyword (name tag))
			       :msg msg
			       :id id}))
      (handle balance-not-zero-error {:keys [tag msg id]}
	      (error/conflict {:type (keyword (name tag))
			       :msg msg
			       :id id}))
      (handle attr-error {:keys [tag msg attr]}
	      (error/bad-request {:type (keyword (name tag))
				  :msg msg
				  :attr attr})))
  (catch IllegalArgumentException e
    (if (re-find #"multimethod" (.getMessage e))
	  (error/not-implemented (.getMessage e))
	  (throw e)))))

(defn requires-authentication? [res-id]
  (not (id/root? (:type res-id))))

(defn perform-action [action res-id db body editor]
  (condp = action
      ;; Collection actions
      :list     (model/list db res-id)
      :add      (success/created (model/add db res-id body editor))
      ;; Entity actions
      :retrieve (model/retrieve db res-id)
      :update   (model/update db res-id body editor)
      :delete   (do (model/delete db res-id editor)
		    (success/deleted (id/parent res-id)))
      (error/internal-server-error (str "Invalid action: " action))))
