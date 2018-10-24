(ns clojure-keybase-proofs.api
  (:require [hugsql.core :as hugsql]
            [clojure.set :refer [rename-keys]]
            [clj-http.client :as client]))

(hugsql/def-db-fns "clojure_keybase_proofs/sql/users.sql")

(defn signup [db username]
  (insert-user db {:username username}))

(defn user [db username]
  (if-let [u (get-user-by-username db {:username username})]
    (assoc u :keybase-users (get-keybase-users-by-username db {:username username}))))

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

(defn keybase-proofs [db username]
  (let [proofs (get-keybase-users-by-username db {:username username})
        formatted-proofs (map #(rename-keys % {:keybase_username :kb_username}) proofs)]
    {:identities {:keybase-proofs formatted-proofs}}))
