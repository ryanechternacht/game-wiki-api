(ns game-wiki-api.database.in-memory-database
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
  (fn []
    (:cards @db)))

(defn read-card-by-id [db]
  (fn [id]
    (get-in @db [:cards id])))

(defn save-card [db]
  (fn [card]
    (do
      (swap! db assoc-in [:cards (:id card)] card))))

(defn get-db-map
  "Takes an atom representing an in memory database. 
   If none is supplied uses the default db atom"
  ([] (get-db-map database))
  ([db]
   {:read-cards (read-cards db)
    :read-card-by-id (read-card-by-id db)
    :save-card (save-card db)}))
