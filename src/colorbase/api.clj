(ns colorbase.api
  (:require [colorbase.db :refer [cmd]]
            [colorbase.util :as util]
            [colorbase.secrets :refer [secrets]]
            [io.keybase.proofs :as keybase-proofs]
            [clojure.set :refer [rename-keys]]
            [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [buddy.core.nonce :as nonce]))

(defn make-jwt-token [username jwt-secret]
  (jwt/sign {:username (keyword username), :exp (util/plus-days (util/now) 1)}
            jwt-secret))

(defn signup [username password jwt-secret]
  (let [hash-options {:alg :bcrypt+sha512, :iterations 12, :salt (nonce/random-bytes 16)}]
    ((:create-user cmd) {:username username
                         :password-hash (hashers/derive password hash-options)})
    (make-jwt-token username jwt-secret)))

(defn signin [{:keys [username password-hash]} password jwt-secret]
  (if (hashers/check password password-hash {:limit #{:bcrypt+sha512}})
    (make-jwt-token username jwt-secret)
    (throw (ex-info "401 Unauthorized. Bad password." {:code 401}))))

(defn login [raw-username raw-password jwt-secret]
  (let [username (util/validate-username raw-username)
        password (util/validate-password raw-password)]
    (if-not (and username password)
      (throw (ex-info "400 Bad Request. Invalid username ([a-f0-9]{6}) or password ([a-f0-9]{18})." {:code 400}))
      (if-some [user ((:get-user-for-auth cmd) {:username username})]
        (signin user password jwt-secret)
        (signup username password jwt-secret)))))

(defn get-user [username]
  (if-some [user ((:get-user cmd) {:username username})]
    user
    (throw (ex-info "404 Not Found." {:code 404}))))

(defn user-exists [username]
  (boolean ((:get-user cmd) {:username username})))

(defn get-user-with-keybase-proofs [username]
  (assoc (get-user username)
         :keybase-proofs ((:get-live-keybase-proofs cmd) {:username username})))

(defn get-keybase-proofs-for-keybase [username]
  (let [proofs ((:get-all-keybase-proofs cmd) {:username username})
        rename-map {:keybase-username :kb_username, :sig-hash :sig_hash}]
    {:signatures (map #(clojure.set/rename-keys % rename-map) proofs)
     :avatar (util/gif-data-url (util/solid-360x360-gif username))}))

(defn get-users-with-live-keybase-proof-count []
  ((:get-users-with-live-keybase-proof-count cmd)))

(defn attempt-to-enliven-proof [domain username keybase-username sig-hash]
  (when (keybase-proofs/proof-live? domain username keybase-username sig-hash)
    ((:enliven-keybase-proof cmd) {:username username :keybase-username keybase-username})))

(defn create-keybase-proof [domain username keybase-username sig-hash]
  ; Check if the proof is valid. If not, error.
  (when-not (keybase-proofs/proof-valid? domain username keybase-username sig-hash)
    (throw (ex-info "401 Unauthorized. Invalid Keybase proof." {:code 403})))
  ; Add the proof to the database.
  ((:create-keybase-proof cmd) {:username username
                                :keybase-username keybase-username
                                :sig-hash sig-hash
                                :is-live false})
  ; Kick off a background task to enliven the proof when it's live on Keybase's side
  (future (util/execute-until-ok
            (partial attempt-to-enliven-proof domain username keybase-username sig-hash)
            some? 10 1000)))

(defn delete-keybase-proof [username keybase-username]
  ((:kill-keybase-proof cmd) {:username username :keybase-username keybase-username}))

(defn kill-keybase-proof-if-needed [domain username keybase-username sig-hash]
  (when-not (keybase-proofs/proof-live? domain username keybase-username sig-hash)
    ((:kill-keybase-proof cmd) {:username username :keybase-username keybase-username})))

(defn update-keybase-proofs [domain username]
  (doall (pmap #(kill-keybase-proof-if-needed domain username (:keybase-username %1) (:sig-hash %1))
               ((:get-live-keybase-proofs cmd) {:username username}))))
