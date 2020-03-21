(ns game-wiki-api.database.mysql-database
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.jdbc :as sql]))

(def db-conn {:subprotocol "mysql"
              :subname "//localhost:8889/game_wiki"
              :user "root"
              :password "root"
              ; removes a warning
              :useSSL false})

;; (sql/query db-conn ["SELECT * FROM person"])

(defn map-rows-to-card [rows]
  (let [card (first rows)]
    {:id (:terraforming_mars_id card)
     :name (:name card)
     :cost (:cost card)
     :tags (vec (map (fn [tag]
                       {:name (:tag_name tag)
                        :value (:tag_value tag)})
                     rows))}))

(def read-cards-query
  "SELECT c.terraforming_mars_id, c.name, c.cost, 
          ct.name as tag_name, ct.value as tag_value
   FROM card c
   LEFT JOIN card_tag ct on c.id = ct.card_id")

(defn read-cards [db]
  (fn []
    (->> (sql/query db [read-cards-query])
         (group-by :terraforming_mars_id)
         vals
         (map map-rows-to-card))))

(defn read-card-by-id-query [id]
  (format "SELECT c.terraforming_mars_id, c.name, c.cost,
                  ct.name as tag_name, ct.value as tag_value
           FROM card c
           LEFT JOIN card_tag ct on c.id = ct.card_id
           WHERE c.terraforming_mars_id = %s", id))

(defn read-card-by-id [db]
  (fn [id]
    (->> (sql/query db [(read-card-by-id-query id)])
         (map-rows-to-card))))

(def read-faqs-simple-query
  "SELECT id, title FROM frequently_asked_question")

(defn read-faqs-simple [db]
  (fn []
    (sql/query db [read-faqs-simple-query])))

(defn get-db-map
  "Takes a connection obj for a mysql db.
   If none is supplied uses the default db connection"
  ([] (get-db-map db-conn))
  ([db]
   {:read-cards (read-cards db)
    :read-card-by-id (read-card-by-id db)
    ;; :create-card! (create-card! db)
    ;; :update-card! (update-card! db)
    :read-faqs-simple (read-faqs-simple db)
    ;; :read-faq-by-id (read-faq-by-id db)
    ;; :search-faqs (search-faqs db)
    ;; :get-popular-faq-tags (get-popular-faq-tags db)
    ;; :update-faq! (update-faq! db)
    ;; :create-faq! (create-faq! db)
    }))