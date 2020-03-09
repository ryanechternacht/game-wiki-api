(ns game-wiki-api.in-memory-database-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.database.in-memory-database :refer :all]))

(deftest read-cards-test
  (testing "Read Cards Test"
    (let [db-val {:cards {1 {:id 1 :name "card 1"} 2 {:id 2 :name "card 2"}}}
          funcs (get-db-map (atom db-val))
          read-cards (:read-cards funcs)]
      (is read-cards "read-cards is defined")
      (let [cards (read-cards)]
        (is (= 2 (count cards)) "Has correct key count")
        (is (= "card 1" (get-in cards [1 :name])) "Card 1 has the correct name")))))

(deftest read-card-by-id-test
  (testing "Read Card By ID Test"
    (let [db-val {:cards {1 {:id 1 :name "card 1"} 2 {:id 2 :name "card 2"}}}
          funcs (get-db-map (atom db-val))
          read-card-by-id (:read-card-by-id funcs)]
      (is read-card-by-id "read-card-by-id is defined")
      (let [card (read-card-by-id 1)]
        (is (= "card 1" (:name card)) "Card has the correct name")))))

(deftest get-next-card-id-test
  (testing "Get Next Card ID Test"
    (testing "simple case"
      (is (= 2 (get-next-card-id {:cards {1 {}}}))))
    (testing "advanced case"
      (is (= 4 (get-next-card-id {:cards {1 {} 2 {} 3 {}}}))))
    (testing "missing ids"
      (is (= 11 (get-next-card-id {:cards {10 {}}}))))
    (testing "empty throws error"
      (is (thrown? Exception (get-next-card-id {}))))))

(deftest save-card-test
  (testing "Save Card Test"
    (let [db-val {:cards {1 {:id 1 :name "card 1"} 2 {:id 2 :name "card 2"}}}
          db (atom db-val)
          funcs (get-db-map db)
          save-card! (:save-card! funcs)]
      (is save-card! "save-card is defined")
      (testing "creating a new card"
        (let [new-card {:name "card 3"}]
          (do
            (save-card! new-card)
            (is (= (:name new-card) (get-in @db [:cards 3 :name])) "Card was added with the correct name")
            (is (= 3 (count (:cards @db))) "New card count is corectly"))))
      (testing "updating an existing card new card"
        (let [new-name "update card 1"
              card {:id 1 :name new-name}
              pre-cards-count (count (:cards @db))]
          (do
            (save-card! card)
            (is (= pre-cards-count (count (:cards @db))) "Card count wasn't changed")
            (is (= new-name (get-in @db [:cards 1 :name])) "Card name was changed")))))))

;; (def funcs (get-db-map database))

;; (def save-card-fn! (:save-card! funcs))

;; (save-card-fn! {:name "hello"})

;; (def get-cards-fn (:read-cards funcs))

;; (get-cards-fn)

;; (apply swap! database assoc-in [[:cards 3] {:id 3 :name "hello"}])

;; (assoc-in @database [:cards 4] {:id 4 :name "hello2"})