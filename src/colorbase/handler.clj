(ns colorbase.handler
  (:require
   [colorbase.views :as views]
   [colorbase.api :as api]
   [colorbase.util :as util]
   [colorbase.middleware :as middleware]
   [colorbase.secrets :refer [secrets]]
   [colorbase.config :refer [config]]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [io.keybase.proofs :as keybase-proofs]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults secure-site-defaults]]
   [ring.util.request :refer [content-type]]
   [ring.util.response :refer [response status redirect]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-response]]))

(defroutes public-routes
  (GET "/" request
       (views/render-homepage (api/get-users-with-live-keybase-proof-count) (:current-username request)))
  (POST "/login" [username password-1 password-2 password-3 :as r]
        (let [username (util/uncolorify username)
              password (apply str (map util/uncolorify [password-1 password-2 password-3]))
              token (api/login username password (:jwt-secret secrets))]
          (middleware/set-auth-cookie (redirect "/" :see-other) token)))
  (GET ["/color/:username", :username util/username-pattern] [username :as request]
       (views/render-profile (= (:current-username request) username)
                             (api/get-user-with-keybase-proofs username)))
  (POST "/api/login" [username password] (response (api/login username password (:jwt-secret secrets))))
  (GET "/api/keybase-proofs" [username] (response (api/get-keybase-proofs-for-keybase username))))

(defn redirect-in-web [request uri]
  (if (= (content-type request) "application/x-www-form-urlencoded")
    (redirect uri :see-other)
    (response nil)))

(defroutes authenticated-routes
  (POST "/logout" request (middleware/unset-auth-cookie (redirect "/" :see-other)))
  (GET "/create-keybase-proof" [keybase-username sig-hash kb-ua username :as request]
       (if (= username (:current-username request))
         (views/render-create-keybase-proof (:current-username request) keybase-username sig-hash kb-ua)
         (throw (ex-info (format "401 Unauthorized. Signed in as %s, but tried to make a Keybase proof for %s"
                                 (:current-username request) username) {:code 401}))))
  (GET "/color" request (redirect (str "/color/" (:current-username request)) :see-other))
  (POST "/api/create-keybase-proof" [keybase-username sig-hash kb-ua username :as request]
        (api/create-keybase-proof
          (:domain-for-keybase config) (:current-username request) keybase-username sig-hash)
        (redirect-in-web request (keybase-proofs/make-proof-success-redirect-link
                                   (:domain-for-keybase config) keybase-username
                                   (:current-username request) sig-hash kb-ua)))
  (POST "/api/delete-keybase-proof" [keybase-username :as request]
        (api/delete-keybase-proof (:current-username request) keybase-username)
        (redirect-in-web request "/color")))

(def common-handler
  (-> (routes public-routes
              (middleware/wrap-require-authentication authenticated-routes)
              (route/resources "/")
              (route/not-found "404 Not Found."))
      middleware/wrap-exceptions
      wrap-json-response
      wrap-json-params
      (middleware/wrap-sessioned-jwt-authentication (:jwt-secret secrets))))

(def http-handler (wrap-defaults common-handler site-defaults))
