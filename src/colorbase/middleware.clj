(ns colorbase.middleware
  (:require
   [colorbase.api :as api]
   [colorbase.config :refer [config]]
   [buddy.auth.backends]
   [buddy.auth.middleware]
   [ring.util.response :refer [response status redirect]]))

(defn wrap-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch clojure.lang.ExceptionInfo e
        (status (response (.getMessage e)) (:code (ex-data e))))
      (catch Exception e
        (prn e)
        (status (response "Internal error.") 500)))))

(defn set-auth-cookie [resp token]
  (assoc-in
    resp
    [:cookies "token"]
    {:value token
     :path "/"
     :max-age 86400}))

(def ^:const expired-token "EXPIRED-TOKEN")
(defn unset-auth-cookie [resp]
  (assoc-in resp [:cookies "token"] expired-token))

(defn wrap-authenticate-jwt-via-cookie [handler]
  (fn [request]
    (if-some [token (get-in request [:cookies "token" :value])]
      (handler (assoc-in request [:headers "Authorization"]
                         (format "Token %s" token)))
      (handler request))))

(defn wrap-consume-authentication [handler]
  (fn [request]
    (if-some [username (get-in request [:identity :username])]
      (if (api/user-exists username)
        (handler (assoc request
                        :current-username username
                        :current-user (api/get-user username)))
        (unset-auth-cookie (redirect "/" :see-other) 401))
      (handler request))))

(defn wrap-require-authentication [handler]
  (fn [request]
    (if (buddy.auth/authenticated? request)
      (handler request)
      (status (response "401 Unauthorized.") 401))))

(defn wrap-sessioned-jwt-authentication [handler jwt-secret]
  (-> handler
      wrap-consume-authentication
      (buddy.auth.middleware/wrap-authentication (buddy.auth.backends/jws {:secret jwt-secret}))
      wrap-authenticate-jwt-via-cookie))
