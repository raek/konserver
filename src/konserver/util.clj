(ns konserver.util
  (:import java.util.Random
	   java.security.MessageDigest
	   org.apache.commons.codec.binary.Base64))

(defn boolean? [x]
  (or (true? x)
      (false? x)))

(defn name-or-string [x]
  (if (keyword? x)
    (name x)
    (str x)))

(defn update
  "'Updates' the values in a map by applying the function of each key-function
  pair with the value corresponding to the key."
  ([map key f]
     (assoc map key (f (get map key))))
  ([map key f & kfs]
     (let [ret (update map key f)]
       (if kfs
	 (recur ret (first kfs) (second kfs) (nnext kfs))
	 ret))))

(defn update-maybe
  ([map key f]
     (if (contains? map key)
       (assoc map key (f (get map key)))
       map))
  ([map key f & kfs]
     (let [ret (update-maybe map key f)]
       (if kfs
	 (recur ret (first kfs) (second kfs) (nnext kfs))
	 ret))))

(defn re-clause-to-if [s [re bindings body] else]
  `(if-let [[~'_ ~@bindings] (re-find ~re ~s)]
     ~body
     ~else))

(defn re-clauses-to-ifs [s clauses]
  (when-first [clause clauses]
	      (if (and (not (next clauses))
		       (= (count clause) 1))
		(first clause)
		(re-clause-to-if s clause (re-clauses-to-ifs s (rest clauses))))))

(defmacro cond-re-let [s & clauses]
  (re-clauses-to-ifs s (partition 3 3 nil clauses)))

(defn md5
  "Compute MD5 sum of a string. The returned value is a string of hexadecimal
   digits."
  [s]
  (let [algorithm (doto (MessageDigest/getInstance "MD5")
		    (.reset)
		    (.update (.getBytes s)))]
    (format "%032x" (BigInteger. 1 (.digest algorithm)))))

(defn b64decode
  "Decodes Base64 encoded data. Returns a byte array, unless charset is given,
   which causes the the byte array to be decoded with the charset."
  ([s]
     (.decode (Base64.) (.getBytes s "US-ASCII")))
  ([s charset]
     (String. (.decode (Base64.) (.getBytes s "US-ASCII")) charset)))

