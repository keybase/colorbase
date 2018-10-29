(ns colorbase.entrypoint
  (:require
    [colorbase.handler :as handler]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn run [handle port]
  (run-jetty handle {:port port}))

(defn run-dev-server [port]
  (run (wrap-reload handler/http-handler) port))

(defn -main [port-str]
  (run handler/https-handler (read-string port-str)))
