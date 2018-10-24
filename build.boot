(set-env!
  :resource-paths #{"src" "resources"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [ring/ring-core "1.6.3"]
                  [ring/ring-jetty-adapter "1.6.3"]
                  [ring/ring-json "0.4.0"]
                  [ring/ring-devel "1.6.3"]
                  [com.layerware/hugsql "0.4.9"]
                  [org.xerial/sqlite-jdbc "3.25.2"]
                  [clj-http "3.9.1"]
                  [compojure "1.6.1"]
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
  [p port PORT int "Server port (default 9000)"]
  (require '[clojure-keybase-proofs.handler :as app])
  (apply (resolve 'app/run-dev-server) [(or port 9000)]))

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
