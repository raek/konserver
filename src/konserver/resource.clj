(ns konserver.resource
  "Type hierarchy for resources")

(derive ::entity ::any)

(doseq [entity [::root ::user ::group ::member ::debt ::debt-rev ::debug]]
  (derive entity ::entity))

(derive ::collection ::any)

(doseq [collection [::user-coll ::group-coll
		    ::member-coll ::debt-coll ::debt-rev-coll ::debug-coll]]
  (derive collection ::collection))

(derive ::error ::any)

(doseq [error [::not-found ::unauthorized ::bad-request ::method-not-allowed
	       ::conflict ::internal-server-error ::not-implemented]]
  (derive error ::error))

(derive ::success ::any)

(doseq [success [::deleted ::created ::no-content]]
  (derive success ::success))
