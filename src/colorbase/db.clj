(ns colorbase.db
  (:require [colorbase.config :refer [config]]
            [colorbase.util :as util]
            [hugsql.core :as hugsql]))

(defn result-one-snake->kebab [this result options]
  (let [row (hugsql.adapter/result-one this result options)]
    (util/map-keys util/keyword-snake->kebab row)))

(defn result-many-snake->kebab [this result options]
  (let [rows (hugsql.adapter/result-many this result options)]
    (map (partial util/map-keys util/keyword-snake->kebab) rows)))

(defmethod hugsql/hugsql-result-fn :1 [sym] 'colorbase.db/result-one-snake->kebab)
(defmethod hugsql/hugsql-result-fn :one [sym] 'colorbase.db/result-one-snake->kebab)
(defmethod hugsql/hugsql-result-fn :* [sym] 'colorbase.db/result-many-snake->kebab)
(defmethod hugsql/hugsql-result-fn :many [sym] 'colorbase.db/result-many-snake->kebab)

(def sqlite-connection
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname (:database-path config)})

(def cmd (util/map-values
           (fn [fn-info] (partial (:fn fn-info) sqlite-connection))
           (hugsql/map-of-db-fns "colorbase/sql/schema.sql")))
