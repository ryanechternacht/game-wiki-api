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

;; card calls
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

(def read-card-by-id-query
  "SELECT c.terraforming_mars_id, c.name, c.cost,
          ct.name as tag_name, ct.value as tag_value
   FROM card c
   LEFT JOIN card_tag ct on c.id = ct.card_id
   WHERE c.terraforming_mars_id = ?")

(defn read-card-by-id [db]
  (fn [id]
    (->> (sql/query db [read-card-by-id-query id])
         (map-rows-to-card))))

;; FAQ calls
(def read-faqs-simple-query
  "SELECT id, title FROM frequently_asked_question")

(defn read-faqs-simple [db]
  (fn []
    (sql/query db [read-faqs-simple-query])))

(def get-popular-faq-tags-query
  "SELECT tag FROM frequently_asked_question_tag
   GROUP BY tag
   HAVING count(*) >= 2
   ORDER BY count(*) desc
   LIMIT 4")

(defn get-popular-faq-tags [db]
  (fn []
    (->> (sql/query db [get-popular-faq-tags-query])
         (map :tag))))

(defn map-rows-to-faq [rows]
  (let [faq (first rows)]
    {:id (:id faq)
     :title (:title faq)
     :body (:body faq)
     :tags (vec (map :tag rows))}))

;; rewrite using group-concat?
(def read-faq-by-id-query
  "SELECT f.id, f.title, f.body, t.tag
   FROM frequently_asked_question f
   LEFT JOIN frequently_asked_question_tag t
   ON f.id = t.frequently_asked_question_id
   WHERE f.id = ?")

(defn read-faq-by-id [db]
  (fn [id]
    (->> (sql/query db [read-faq-by-id-query id])
         (map-rows-to-faq))))

(defn search-faqs-query [term]
  (format "SELECT f.id, f.title
           FROM frequently_asked_question f
           LEFT JOIN frequently_asked_question_tag t
           ON f.id = t.frequently_asked_question_id
           WHERE f.title LIKE '%1$s%%'
               OR f.body LIKE '%1$s%%'
               OR t.tag LIKE '%1$s%%'"
          term))

(defn search-faqs [db]
  (fn [term]
    (sql/query db [(search-faqs-query term)])))

(defn update-faq! [db]
  (fn [faq]
    (do
      (let [id (:id faq)]
        (sql/update! db :frequently_asked_question
                     (select-keys faq [:title :body])
                     ["id = ?" id])
        (sql/delete! db :frequently_asked_question_tag
                     ["frequently_asked_question_id = ?" id])
        (sql/insert-multi! db :frequently_asked_question_tag
                           [:frequently_asked_question_id :tag]
                           (vec (map (fn [tag] [id tag]) (:tags faq)))))
      faq)))

(defn create-faq! [db]
  (fn [faq]
    (do
      (let [result (sql/insert! db :frequently_asked_question
                                (select-keys faq [:title :body]))
            id (:generated_key (first result))]
        (sql/insert-multi! db :frequently_asked_question_tag
                           [:frequently_asked_question_id :tag]
                           (vec (map (fn [tag] [id tag]) (:tags faq))))
        (assoc faq :id id)))))

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
    :read-faq-by-id (read-faq-by-id db)
    :search-faqs (search-faqs db)
    :get-popular-faq-tags (get-popular-faq-tags db)
    :update-faq! (update-faq! db)
    :create-faq! (create-faq! db)}))