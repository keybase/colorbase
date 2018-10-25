(set-env!
  :resource-paths #{"src" "resources"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [ring/ring-core "1.7.0"]
                  [ring/ring-jetty-adapter "1.6.3"]
                  [ring/ring-json "0.4.0"]
                  [ring/ring-devel "1.6.3"]
                  [com.layerware/hugsql "0.4.9"]
                  [org.xerial/sqlite-jdbc "3.25.2"]
                  [clj-http "3.9.1"]
                  [compojure "1.6.1"]
                  ; TODO rm dependency with buddy crore
                  [crypto-random "1.2.0"]
                  [buddy/buddy-auth "2.1.0"]
                  [buddy/buddy-hashers "1.3.0"]
                  [hiccup "1.0.5"]])

(task-options!
  pom  {:project 'colorbase
        :version "0.1.0"})

(deftask run-server
  "Run server"
  [p port PORT int "Server port (default 9000)"]
  (require '[clojure-keybase-proofs.handler :as app])
  (apply (resolve 'app/run-server) [(or port 9000)]))

(deftask run-dev-server
  "Run server hot reloading Clojure namespaces"
  [p port PORT int "Server port (default 8000)"]
  (require '[clojure-keybase-proofs.handler :as app])
  (apply (resolve 'app/run-dev-server) [(or port 8000)]))

(deftask refresh-db
  "Drop and create all tables."
  [d db-path VAL str "Path to sqlite database"]
  (let [db {:classname "org.sqlite.JDBC"
            :subprotocol "sqlite"
            :subname db-path}]
    (require '[clojure-keybase-proofs.api :as api])
    ((resolve 'api/drop-users-table) db)
    ((resolve 'api/drop-keybase-users-table) db)
    ((resolve 'api/create-users-table) db)
    ((resolve 'api/create-keybase-users-table) db)))

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
   (aot :namespace #{'clojure-keybase-proofs.entrypoint})
   (uber)
   (jar :file "colorbase-app.jar" :main 'clojure-keybase-proofs.entrypoint)
   (sift :include #{#"colorbase-app.jar"})
   (target)))

(deftask make-secrets-config
  "Makes a sample secrets file encrypted with Keybase."
  [n keybase-name VAL str   "Keybase username or team name to encrypt for"
   o out-filename VAL str   "Name of the encrypted config file to write to."
   t for-team         bool  "Enable this to encrypt for a team."]
  (require '[crypto.random :as csprng])
  (let [secrets (prn-str {:jwt-secret ((resolve 'csprng/hex) 64)})
        encrypt-cmd-fmt "keybase encrypt %s --no-device-keys --no-paper-keys%s -m"
        encrypt-cmd (format encrypt-cmd-fmt keybase-name (if for-team " --team" ""))
        _ (prn encrypt-cmd)
        encrypt-cmd-args (concat (clojure.string/split encrypt-cmd #" ") [secrets])
        encrypt-res (apply clojure.java.shell/sh encrypt-cmd-args)
        _ (assert (zero? (:exit encrypt-res)) (:err encrypt-res))
        encrypted-secrets (:out encrypt-res)]
    (spit out-filename encrypted-secrets)))
