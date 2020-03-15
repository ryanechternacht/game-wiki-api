(ns game-wiki-api.database.in-memory-database
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def initial-data-file "./resources/initial_data.edn")

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))))

(defonce database (atom (load-edn initial-data-file)))

;; cards calls
(defn read-cards [db]
  (fn []
    (vals (:cards @db))))

(defn read-card-by-id [db]
  (fn [id]
    (get-in @db [:cards id])))

(defn get-next-card-id [db-val]
  (inc
   (reduce max (keys (:cards db-val)))))

(defn save-card! [db]
  (fn [card]
    (do
      (swap! db (fn [db-val]
                  (do
                    (let [c (if (:id card)
                              card
                              (assoc card :id (get-next-card-id db-val)))]
                      (assoc-in db-val [:cards (:id c)] c))))))))

;; faq calls
(defn read-faq-by-id [db]
  (fn [id]
    (get-in @db [:faqs id])))

(defn read-faqs-simple [db]
  (fn []
    (map #(select-keys % [:title :id]) (vals (:faqs @db)))))

;; add in tags searching
(defn search-faqs [db]
  (fn [query]
    (->> (:faqs @db)
         vals
         (filter #(or (str/includes? (:title % "") query)
                      (str/includes? (:body % "") query)
                      (some (fn [t] (str/includes? t query)) (:tags % []))))
         (map #(select-keys % [:title :id])))))

; return tags with at least 2 hits, max 4
(defn get-popular-faq-tags [db]
  (fn []
    (->> (:faqs @db)
         vals
         (mapcat (fn [m] (:tags m)))
         (reduce #(assoc %1 %2 (inc (get %1 %2 0))) {})
         (filter #(> (second %) 1))
         (sort-by second >)
         (take 4)
         (map first))))

(defn get-db-map
  "Takes an atom representing an in memory database. 
   If none is supplied uses the default db atom"
  ([] (get-db-map database))
  ([db]
   {:read-cards (read-cards db)
    :read-card-by-id (read-card-by-id db)
    :save-card! (save-card! db)
    :read-faqs-simple (read-faqs-simple db)
    :read-faq-by-id (read-faq-by-id db)
    :search-faqs (search-faqs db)
    :get-popular-faq-tags (get-popular-faq-tags db)}))

