(ns game-wiki-api.database.in-memory-database
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

;; TODO this should somehow be rewritten to allow us to inject
;; the databsae as we go

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

