(ns game-wiki-api.domain-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.domain :refer :all]))

(deftest validate-card-test
  (testing "Validate Card Test"
    (testing "valid cards"
      (let [card {:id 1 :name "name"}]
        (is (validate-card card))))
    (testing "invalid cards"
      (is (not (validate-card {:id 1})) "missing name")
      (is (not (validate-card {:name "hello"})) "missing id"))))

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

;; relies on get-next-card-id-test
(deftest make-card-test
  (testing "Make card"
    (let [card-name "hello, world"
          card (make-card {:cards {1 {}}} {:name card-name})]
      (is card "card was created")
      (is (= 2 (:id card)) "id was attached correctly")
      (is (= card-name (:name card))))))