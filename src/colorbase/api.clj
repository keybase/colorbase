(ns colorbase.api
  (:require [colorbase.db :refer [cmd]]
            [colorbase.util :as util]
            [colorbase.secrets :refer [secrets]]
            [org.keybase.proofs :as keybase-proofs]
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
         :keybase-proofs ((:get-keybase-proofs cmd) {:username username})))

(defn get-keybase-proofs-for-keybase [username]
  (let [proofs (:keybase-proofs (get-user-with-keybase-proofs username))
        rename-map {:keybase-username :kb_username, :sig-hash :sig_hash}]
    (map #(clojure.set/rename-keys % rename-map) proofs)))

(defn get-users-with-live-keybase-proof-count []
  ((:get-users-with-live-keybase-proof-count cmd)))

(defn create-keybase-proof [domain username keybase-username sig-hash]
  ; Check if the proof is valid. If not, error.
  (when-not (keybase-proofs/valid-proof? domain username keybase-username sig-hash)
    (throw (ex-info "401 Unauthorized. Invalid Keybase proof." {:code 403})))
  ; Add the proof to your database.
  ((:create-keybase-proof cmd) {:username username
                                :keybase-username keybase-username
                                :sig-hash sig-hash
                                :is-live false})
  ; At this point, requesting /api/keybase-proofs returns this proof, but it
  ; isn't marked as live yet, and so it isn't shown on users profiles.
  ; Now, check if the proof is live.
  (if (keybase-proofs/proof-live? domain username keybase-username sig-hash)
    ; If it is, mark the proof as live and start showing it on the user's profile pages.
    ((:enliven-keybase-proof cmd) {:username username
                                   :keybase-username keybase-username})
    ; Otherwise, keep it dead and return an error.
    (throw (ex-info "401 Unauthorized. Keybase proof not live." {:code 401}))))

(defn delete-keybase-proof [username keybase-username]
  ((:kill-keybase-proof cmd) {:username username :keybase-username keybase-username}))
