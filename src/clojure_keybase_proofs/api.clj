(ns clojure-keybase-proofs.api
  (:require [hugsql.core :as hugsql]
            [clj-http.client :as client]))

(hugsql/def-db-fns "clojure_keybase_proofs/sql/users.sql")

(defn signup [db username]
  (insert-user db {:username username}))

(defn user [db username]
  (get-user-by-username db {:username username}))

(defn check-keybase-proof [keybase-username sig-hash]
  (let [check-url "https://keybase.io/_/api/1.0/sig/check_proof.json"
        params {:kb_username keybase-username :sig_hash sig-hash}
        resp (client/get check-url {:query-params params :as :json-strict})]
    (:proof_ok resp)))

(defn prove-keybase-identity [db {:keys [username keybase-username sig-hash] :as params}]
  (if (and (user db username) (check-keybase-proof keybase-username sig-hash))
    (set-keybase-username db params)))

(defn keybase-proofs [db username]
  (let [proofs (get-keybase-users-by-username db {:username username})]
    {:identities {:keybase-proofs proofs}}))
