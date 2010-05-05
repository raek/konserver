(ns konserver.date
  (:import [java.util Calendar TimeZone]))

(defstruct date :year :month :day :hour :minute :second)

(defn make-date
  "Creates a datetime in the form of a map. Keys are year, month, day, hour,
  minute and second, and the time is in UTC."
  [year month day hour minute second]
  (struct date year month day hour minute second))

(defn now
  "Returns the current datetime in UTC."
  []
  (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
    (make-date (.get cal Calendar/YEAR) (inc (.get cal Calendar/MONTH))
	       (.get cal Calendar/DAY_OF_MONTH) (.get cal Calendar/HOUR_OF_DAY)
	       (.get cal Calendar/MINUTE) (.get cal  Calendar/SECOND))))

(defn format-date
  "Formats a datetime to the \"Web Datetime Format\". For details see
  <http://www.w3.org/TR/NOTE-datetime>."
  [date]
  (format "%04d-%02d-%02dT%02d:%02d:%02dZ"
	  (:year date) (:month date) (:day date)
	  (:hour date) (:minute date) (:second date)))

(defn human-format-date
  "Formats a datetime a format similar to the \"Web Datetime Format\"."
  [date]
  (format "%04d-%02d-%02d %02d:%02d:%02d UTC"
	  (:year date) (:month date) (:day date)
	  (:hour date) (:minute date) (:second date)))

(defn parse-date
  "Parses a datetime from the \"Web Datetime Format\". For details see
  <http://www.w3.org/TR/NOTE-datetime>."
  [string]
  (when-let [match (re-find #"([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})Z" string)]
    (apply make-date (map #(Integer. %) (rest match)))))

(defn compare-dates
  "Returns -1, 0 or 1 if the first date is before, the same as or after the
   second date."
  [d1 d2]
  (let [date-vec #(vec (map % [:year :month :day :hour :second :minute]))]
    (compare (date-vec d1) (date-vec d2))))

(defn earliest
  "Returns the earliest of the dates."
  ([d]
     d)
  ([d1 d2]
     (if (neg? (compare-dates d1 d2)) d1 d2))
  ([d1 d2 & ds]
     (reduce earliest (earliest d1 d2) ds)))

(defn latest
  "Returns the latest of the dates."
  ([d]
     d)
  ([d1 d2]
     (if (pos? (compare-dates d1 d2)) d1 d2))
  ([d1 d2 & ds]
     (reduce latest (latest d1 d2) ds)))
