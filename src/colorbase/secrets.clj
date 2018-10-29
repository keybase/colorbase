(ns colorbase.secrets
  (:require [colorbase.config :refer [config]]
            [clojure.java.shell]))

(def secrets-res (clojure.java.shell/sh "keybase" "decrypt" "-i" (:secrets-path config)))
(assert (zero? (:exit secrets-res)) (:err secrets-res))
(def secrets (read-string (:out secrets-res)))
