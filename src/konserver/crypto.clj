(ns konserver.crypto
  (:import java.util.Random
	   java.security.MessageDigest))

(defn sha256
  "Compute the SHA-256 hash of a byte array. Returns 32 bytes."
  [byte-array]
  (let [algorithm (doto (MessageDigest/getInstance "SHA-256")
		    (.reset)
		    (.update byte-array))]
    (.digest algorithm)))

(def hex-digits "0123456789abcdef")

(defn hex-to-bytes [hex-string]
  (if (odd? (.length hex-string))
    (recur (str "0" hex-string))
    (byte-array
     (for [[high low] (partition 2 hex-string)]
       (byte (+ (* 16 (.indexOf hex-digits (int high)))
		(.indexOf hex-digits (int low))))))))

(defn byte-to-hex [byte]
  (let [value (bit-and byte 0xff)]
    (str (nth hex-digits (/   value 16))
	 (nth hex-digits (mod value 16)))))

(defn bytes-to-hex [byte-array]
  (reduce str (map byte-to-hex byte-array)))

(defn concat-bytes [& byte-arrays]
  (byte-array (apply concat byte-arrays)))

(defn random-salt
  "Returns eight random bytes."
  []
  (hex-to-bytes (format "%016x" (.nextLong (Random.)))))

(defn make-password-hash-and-salt [password]
  (let [password-bytes      (.getBytes password "UTF-8")
	salt-bytes          (random-salt)
	salt-hex            (bytes-to-hex salt-bytes)
	password-hash-bytes (sha256 (concat-bytes password-bytes salt-bytes))
	password-hash-hex   (bytes-to-hex password-hash-bytes)]
    [password-hash-hex salt-hex]))
  
(defn valid-password? [password password-hash-hex password-salt-hex]
  (let [password-bytes (.getBytes password "UTF-8")
	salt-bytes     (hex-to-bytes password-salt-hex)
	hash-bytes     (sha256 (concat-bytes password-bytes salt-bytes))
	hash-hex       (bytes-to-hex hash-bytes)]
    (= password-hash-hex hash-hex)))
