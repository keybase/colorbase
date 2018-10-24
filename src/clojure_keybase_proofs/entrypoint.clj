(ns clojure-keybase-proofs.entrypoint
  (:gen-class))

(defn -main [port]
  (require '[clojure-keybase-proofs.handler :as handler])
  ((resolve 'handler/run-server) port))
