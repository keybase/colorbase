(ns clojure-keybase-proofs.api
  (:require [hugsql.core :as hugsql]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [buddy.sign.jwt :as jwt]
            [clj-http.client :as client]))

(hugsql/def-db-fns "clojure_keybase_proofs/sql/users.sql")

(defn validate-username [s]
  (re-matches #"[a-f0-9]{6}" (string/lower-case s)))

(defn validate-password [s]
  (re-matches #"[a-f0-9]{12}" (string/lower-case s)))

(defn now [] (new java.util.Date))
(defn plus-days [dt n] (.plus (.toInstant dt) n java.time.temporal.ChronoUnit/DAYS))

(defn signup [db raw-username raw-password jwt-secret]
  (if-let [username (validate-username raw-username)]
    (if-let [password (validate-password raw-password)]
      (do (insert-user db {:username username :password-hash "hello" :salt "goodbye"})
          (jwt/sign {:user (keyword username) :exp (plus-days (now) 1)} jwt-secret) ; todo CONFIGexp
    ))))

(defn uncolorify [color-hexstring]
  (subs color-hexstring 1))

(defn signup-form [db raw-username raw-password-1 raw-password-2 jwt-secret]
  (signup
    db
    (uncolorify raw-username)
    (str (uncolorify raw-password-1) (uncolorify raw-password-2))
    jwt-secret))

(defn user [db username]
  (when-let [u (get-user-by-username db {:username username})]
    (assoc u :keybase-users (get-keybase-users-by-username db {:username username}))))

(defn users [db]
  (let [us (get-all-users db)]
    (map (comp (partial user db) :username) us)))

(defn check-keybase-proof [keybase-username sig-hash]
  (let [check-url "https://keybase.io/_/api/1.0/sig/check_proof.json"
        params {:kb_username keybase-username :sig_hash sig-hash}
        resp (client/get check-url {:query-params params :as :json-strict})]
    (get-in resp [:body :proof_ok])))

(defn prove-keybase-identity [db {:keys [username keybase-username sig-hash] :as params}]
  (if (user db username)
    (let [_ (set-keybase-username db params)
          proof-ok (check-keybase-proof keybase-username sig-hash)]
      (when-not proof-ok
        (unset-keybase-username db params))
      proof-ok)))

(defn delete-keybase-identity [db username keybase-username])

(defn keybase-proofs [db username]
  (let [proofs (get-keybase-users-by-username db {:username username})
        formatted-proofs (map #(rename-keys % {:keybase_username :kb_username}) proofs)]
    {:identities {:keybase-proofs formatted-proofs}}))
