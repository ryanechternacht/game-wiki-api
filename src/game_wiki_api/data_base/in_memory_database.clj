(ns game-wiki-api.data-base.in-memory-database
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def initial-data-file "./resources/initial_data.edn")

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))))

(defonce database (atom (load-edn initial-data-file)))

(defn read-cards [db]
  (:cards db))

(defn read-card-by-id [db id]
  (get-in db [:cards id]))

(defn attach-db-fn [context]
  (assoc-in context [:request :database] @database))

(defn commit-transaction-fn [context]
  (if-let [[op & args] (:tx-data context)]
    (do
      (apply swap! database op args)
      (assoc-in context [:request :database] @database))
    context))

(def db-interceptor
  {:name :db-interceptor
   :enter attach-db-fn
   :leave commit-transaction-fn})
