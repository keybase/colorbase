(ns clojure-keybase-proofs.handler
  (:require
   [clojure-keybase-proofs.views :as views]
   [clojure-keybase-proofs.api :as api]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [compojure.handler]
   [ring.util.response :refer [response status]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]))

(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "database.db"})

; parameterize over db?
(defroutes app-routes
  (GET "/" [] (views/homepage))
  (GET "/make-keybase-proof" [keybase-username sig-hash]
       (let [url "https://colorbase.modalduality.org/api/prove-keybase-identity username=<USERNAME> keybase-username=%s sig-hash=%s"]
         (response (format (str "$ http POST " url) keybase-username sig-hash))))
  (GET "/api/user" [username]
       (if-let [user (api/user db username)]
         (response user)))
  (POST "/api/signup" request
       (api/signup db (get-in request [:body :username]))
       (response nil))
  (POST "/api/prove-keybase-identity" request
        (if (api/prove-keybase-identity db (:body request))
          (response nil)
          (status (response nil) 403)))
  (GET "/api/keybase-proofs" [username]
       (if (api/user db username)
         (response (api/keybase-proofs db username))
         (status (response nil) 404)))
  (route/resources "/")
  (route/not-found "404 Not Found"))

(def handler
  (-> #'app-routes
      wrap-json-response
      compojure.handler/api ; this is deprecated, use ring-defaults
      (wrap-json-body {:keywords? true})))

(def dev-handler
  (wrap-reload handler))

(defn run-server [port] (run-jetty handler {:port port}))

(defn run-dev-server [port] (run-jetty dev-handler {:port port}))
