(ns colorbase.util
  (:require
   [clojure.string :as string]
   [crypto.random :as csprng]))

(defn map-values [f m]
  (reduce-kv (fn [m k v] (assoc m k (f v))) nil m))

(defn map-keys [f m]
  (reduce-kv (fn [m k v] (assoc m (f k) v)) nil m))

(def username-pattern #"[a-f0-9]{6}")

(def password-pattern #"[a-f0-9]{18}")

(defn validate-username [s]
  (and (string? s) (re-matches username-pattern s)))

(defn validate-password [s]
  (and (string? s) (re-matches password-pattern s)))

(defn uncolorify [color-hexstring]
  (and (string? color-hexstring)
       (pos? (count color-hexstring))
       (subs (string/lower-case color-hexstring) 1)))

(defn colorify [username] (str "#" username))

(defn now [] (new java.util.Date))

(defn plus-days [dt n] (.plus (.toInstant dt) n java.time.temporal.ChronoUnit/DAYS))

(defn keyword-snake->kebab [snake]
  (-> snake
      name
      (clojure.string/replace "_" "-")
      keyword))

(defn random-color []
  (str "#" (csprng/hex 3)))

(defn execute-until-ok [f ok? max-tries delay-ms]
  (when (and (pos? max-tries) ((complement ok?) (f))) ; short-circuiting!
    (Thread/sleep delay-ms)
    (execute-until-ok f ok? (dec max-tries) delay-ms)))
