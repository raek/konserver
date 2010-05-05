(ns konserver.model.math
  (:use clojure.test))

(defn abs
  "Absolute value of a number."
  [x]
  (if (< x 0)
    (- x)
    x))

(defn frac
  "Fraction part of a number. The result is always in the interval [0,1["
  [x]
  (- x (int x)))

(defn sign
  "Sign of number. Returns -1, 0 or 1."
  [x]
  (cond (neg? x) -1
	(zero? x) 0
	(pos? x) 1))

(with-test
    (defn round-half-to-even
      "Rounds a number to the closest integer, using round half to even as
      tie-breaking rule."
      [x]
      (let [xf (abs (frac x))
	    xi (abs (int x))]
	(* (sign x)
	   (cond (< xf 1/2) xi
		 (> xf 1/2) (inc xi)
		 (= xf 1/2) (if (even? xi)
			      xi
			      (inc xi))))))
  (are [x y] (= (round-half-to-even x) y)
       3.0  3    4.0  4   -3.0 -3   -4.0 -4
       3.2  3    4.2  4   -3.2 -3   -4.2 -4
       3.5  4    4.5  4   -3.5 -4   -4.5 -4
       3.8  4    4.8  5   -3.8 -4   -4.8 -5))

(defn nearest-numerator
  "Calculates the multiple of div that is closest to num.
  
  (nearest-numerator 100 3) => 99"
  [num div]
  (* div (round-half-to-even (/ num div))))
