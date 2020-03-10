(ns game-wiki-api.domain-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.domain :refer :all]))

(deftest validate-new-card-test
  (testing "Validate New Card Test"
    (testing "valid cards"
      (let [card {:name "name"}]
        (is (validate-new-card card))))
    (testing "invalid cards"
      (is (not (validate-new-card {:other "other"})) "missing name")
