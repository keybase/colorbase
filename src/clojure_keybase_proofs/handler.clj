(ns clojure-keybase-proofs.handler
  (:require
   [clojure-keybase-proofs.views :as views]
   [clojure-keybase-proofs.api :as api]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [compojure.handler]
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.auth.backends]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [ring.util.response :refer [response status bad-request redirect]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]))


(def config (read-string (slurp "resources/config.edn")))
(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname (:database-path config)})

(def secrets-res (clojure.java.shell/sh "keybase" "decrypt" "-i" "resources/secrets.edn.saltpack"))
(assert (zero? (:exit secrets-res)) (:err secrets-res))
(def jwt-secret (:jwt-secret (read-string (:out secrets-res))))
(def backend (buddy.auth.backends/jws {:secret jwt-secret}))

; TODO other form of raise? https://github.com/ring-clojure/ring/blob/1.7.0/ring-core/src/ring/middleware/cookies.clj#L137
(defn wrap-authenticate-jwt-via-cookie [handler]
  (fn [request]
    (let [token-string (format "Token %s" (get-in request [:cookies "token" :value]))]
      (handler (assoc-in request [:headers "Authorization"] token-string)))))

(defn set-auth-cookie [resp token]
  (assoc-in
    resp
    [:cookies "token"]
    {:value token
     ; :secure true TODO Configure
     ; :domain "127.0.0.1:8000" TODO configure
     :path "/"
     :max-age 86400}))

; parameterize over db? just make the handler at runtime without using defroutes
; split into need auth and not use two defroutes for sec bros!
(defroutes app-routes
  (GET "/" [] (views/homepage (api/users db)))
  (GET "/whoami" request
       (do
         (prn request)
         (prn (:cookies request))
         (prn (authenticated? request))
         (prn (:identity request)))
       (response (:cookies request))
       )
  (GET "/u/:username" [username]
       (if-let [user (api/user db username)]
         (views/profile true user)))
  (POST "/" [username password-1 password-2]
       (if-let [token (api/signup-form db username password-1 password-2 jwt-secret)]
         (let [resp (set-auth-cookie (redirect "/" :see-other) token)]
           (prn resp)
           resp)
         (bad-request "400 Bad Request.")))
  (GET "/make-keybase-proof" [keybase-username sig-hash]
       (views/render-make-keybase-proof keybase-username sig-hash))
  (GET "/api/user" [username]
       (if-let [user (api/user db username)]
         (response user)))
  (POST "/api/signup" {:keys [body]}
       (if-let [token (api/signup db (:username body) (:password body) jwt-secret)]
         (set-auth-cookie (response nil) token)
         (bad-request "400 Bad Request.")))
  (POST "/api/prove-keybase-identity" request
        (if (api/prove-keybase-identity db (:body request))
          (response nil)
          (status (response nil) 403)))
  (POST "/api/delete-keybase-identity" {:keys [body]}
        (if (api/delete-keybase-identity db (:username body) (:keybase-username body))
          (response nil)
          (status (response nil) 403)))
  (GET "/api/keybase-proofs" [username]
       (if (api/user db username)
         (response (api/keybase-proofs db username))
         (status (response nil) 404)))
  (route/resources "/")
  (route/not-found "404 Not Found."))

(def handler
  (-> #'app-routes
      (wrap-authentication backend)
      wrap-authenticate-jwt-via-cookie
      wrap-cookies
      wrap-json-response
      compojure.handler/api ; this is deprecated, use ring-defaults
      (wrap-json-body {:keywords? true})))
(defn run-server [port] (run-jetty handler {:port port}))

(def dev-handler (wrap-reload handler))
(defn run-dev-server [port] (run-jetty dev-handler {:port port}))
