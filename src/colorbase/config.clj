(ns colorbase.config)

(def config (read-string (slurp "resources/config.edn")))
