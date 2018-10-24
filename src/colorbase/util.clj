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

; https://stackoverflow.com/a/15627016
(defn unhexify "Convert hex string to byte sequence" [s]
  (letfn [(unhexify-2 [c1 c2]
            (unchecked-byte
              (+ (bit-shift-left (Character/digit c1 16) 4)
                 (Character/digit c2 16))))]
    (map #(apply unhexify-2 %) (partition 2 s))))

; https://stackoverflow.com/a/15627016
(defn unhexify-str [s]
  (apply str (map char (unhexify s))))

(defn solid-360x360-gif [color-hexstring]
  (unhexify
	(str "47494638396168016801f00000"
		 color-hexstring
		 "00000021f90400000000002c00000000680168010002fe848fa9cbed0fa39cb4da8bb3debcfb0f86e24896e689a6eacab6ee0bc7f24cd7f68de7facef7fe0f0c0a87c4a2f1884c2a97cca6f3098d4aa7d4aaf58acd6ab7dcaef70b0e8bc7e4b2f98c4eabd7ecb6fb0d8fcbe7f4bafd8ecfebf7fcbeff0f182838485868788898a8b8c8d8e8f808192939495969798999a9b9c9d9e9f9091a2a3a4a5a6a7a8a9aaabacadaeafa0a1b2b3b4b5b6b7b8b9babbbcbdbebfb0b1c2c3c4c5c6c7c8c9cacbcccdcecfc0c1d2d3d4d5d6d7d8d9dadbdcdddedfd0d1e2e3e4e5e6e7e8e9eaebecedeeefe0e1f2f3f4f5f6f7f8f9fafbfcfdfefff0f30a0c081040b1a3c8830a1c2850c1b3a7c0831a2c489142b5abc8831a3c6fe8d1c3b7afc0832a4c891244b9a3c8932a5ca952c5bba7c0933a6cc99346bdabc8933a7ce9d3c7bfafc0934a8d0a1448b1a3d8a34a9d2a54c9b3a7d0a35aad4a954ab5abd8a35abd6ad5cbb7afd0a36acd8b164cb9a3d8b36addab56cdbba7d0b37aedcb974ebdabd8b37afdebd7cfbfafd0b38b0e0c1840b1b3e8c38b1e2c58c1b3b7e0c39b2e4c9942b5bbe8c39b3e6cd9c3b7bfe0c3ab4e8d1a44b9b3e8d3ab5ead5ac5bbb7e0d3bb6ecd9b46bdbbe8d3bb7eeddbc7bfbfe0d3cb8f0e1c48b1b3f8e3cb9f2e5cc9b3b7f0e3dbaf4e9d4ab5bbf8e3dbbf6eddcbb7bff0e3ebcf8f1e4cb9b3f8f3ebdfaf5ecdbbb7f0f3fbefcf9f4ebdbbf8f3fbffefdfc08fbfbff0fa04f0500003b")))

(defn gif-data-url [gif-bytes]
  (str "data:image/gif;base64," (.encodeToString (java.util.Base64/getEncoder) (byte-array gif-bytes))))
