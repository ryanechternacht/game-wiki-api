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
              :use-ssl false})

;; (sql/query db-conn ["SELECT * FROM person"])
;; 

(def read-cards-query
  "SELECT c.terraforming_mars_id, c.name, c.cost, 
          ct.name as tag_name, ct.value as tag_value
   FROM card c
   JOIN card_tag ct on c.id = ct.card_id")

(defn read-cards [db]
  (fn []
    (->> (sql/query db [read-cards-query])
         (group-by :terraforming_mars_id)
         vals
         (map (fn [rows]
                (let [card (first rows)]
                  {:id (:terraforming_mars_id card)
                   :name (:name card)
                   :cost (:cost card)
                   :tags (vec (map (fn [tag]
                                     {:name (:tag_name tag)
                                      :value (:tag_value tag)})
                                   rows))}))))))

(defn get-db-map
  "Takes an atom representing an in memory database. 
   If none is supplied uses the default db atom"
  ([] (get-db-map db-conn))
  ([db]
   {:read-cards (read-cards db)
    ;; :read-card-by-id (read-card-by-id db)
    ;; :create-card! (create-card! db)
    ;; :update-card! (update-card! db)
    ;; :read-faqs-simple (read-faqs-simple db)
    ;; :read-faq-by-id (read-faq-by-id db)
    ;; :search-faqs (search-faqs db)
    ;; :get-popular-faq-tags (get-popular-faq-tags db)
    ;; :update-faq! (update-faq! db)
    ;; :create-faq! (create-faq! db)
    }))