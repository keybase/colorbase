(set-env!
  :resource-paths #{"src" "resources"}
  :dependencies '[[org.clojure/clojure "1.9.0"]
                  [ring/ring-core "1.7.0"]
                  [ring/ring-jetty-adapter "1.6.3"]
                  [ring/ring-json "0.4.0"]
                  [ring/ring-defaults "0.3.2"]
                  [ring/ring-devel "1.6.3"]
                  [com.layerware/hugsql "0.4.9"]
                  [org.xerial/sqlite-jdbc "3.25.2"]
                  [compojure "1.6.1"]
                  [crypto-random "1.2.0"]
                  [buddy/buddy-auth "2.1.0"]
                  [buddy/buddy-hashers "1.3.0"]
                  [hiccup "1.0.5"]
                  [clj-keybase-proofs "0.1.0"]]
  :checkouts '[[clj-keybase-proofs "0.1.0"]])

(task-options!
  pom  {:project 'colorbase
        :version "0.1.0"})

(deftask run-dev-server
  [p port PORT int "Server port"]
  (require 'colorbase.entrypoint)
  ((resolve 'colorbase.entrypoint/run-dev-server) port))

(deftask refresh-db
  []
  (require '[colorbase.db :refer [cmd]])
  (let [cmd (deref (resolve 'cmd))]
    ((:drop-users-table cmd))
    ((:drop-keybase-proofs-table cmd))
    ((:create-users-table cmd))
    ((:create-keybase-proofs-table cmd))))

(deftask build
  []
  (comp
   (aot :namespace #{'colorbase.entrypoint})
   (uber)
   (jar :file "colorbase-app.jar" :main 'colorbase.entrypoint)
   (sift :include #{#"colorbase-app.jar"})
   (target)))

(deftask make-secrets-config
  [n keybase-name VAL str   "Keybase username or team name to encrypt for"
   o out-filename VAL str   "Name of the encrypted config file to write to."
   t for-team         bool  "Enable this to encrypt for a team."]
  (require '[crypto.random :as csprng])
  (let [secrets (prn-str {:jwt-secret ((resolve 'csprng/hex) 64)})
        encrypt-cmd-fmt "keybase encrypt %s --no-paper-keys%s -m"
        encrypt-cmd (format encrypt-cmd-fmt keybase-name (if for-team " --team" ""))
        encrypt-cmd-args (concat (clojure.string/split encrypt-cmd #" ") [secrets])
        encrypt-res (apply clojure.java.shell/sh encrypt-cmd-args)
        _ (assert (zero? (:exit encrypt-res)) (:err encrypt-res))
        encrypted-secrets (:out encrypt-res)]
    (spit out-filename encrypted-secrets)))
