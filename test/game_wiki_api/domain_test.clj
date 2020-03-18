(ns game-wiki-api.domain-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.domain :refer :all]))

(deftest validate-new-card-test
  (testing "Validate New Card Test"
    (testing "valid cards"
      (let [card {:name "name" :extra "extra"}]
        (is (validate-new-card card) "validates legal card")))
    (testing "invalid cards"
      (is (not (validate-new-card {:other "other"})) "missing name"))))

(deftest validate-update-card-test
  (testing "Validate Update Card Test"
    (let [card {:name "name" :extra "extra" :id 1}]
      (testing "valid cards"
        (is (validate-update-card card) "validates legal card"))
      (testing "invalid cards"
        (is (not (validate-update-card (dissoc card :id))) "card requires id")
        (is (not (validate-update-card (dissoc card :name))) "card require name")))))

(deftest validate-new-faq-test
  (testing "Validate New Faq Test"
    (let [faq {:title "title" :body "body" :tags [] :extra "extra"}]
      (testing "valid faqs"
        (is (validate-new-faq faq) "validates legal faq"))
      (testing "invalid faqs"
        (is (not (validate-new-faq (dissoc faq :title))) "should require :title")
        (is (not (validate-new-faq (dissoc faq :body))) "should require :body")
        (is (not (validate-new-faq (dissoc faq :tags))) "should require :tags")))))

(deftest supply-default-faq-fields-test
  (testing "Supply Default Faq Fields Test"
    (testing "supplies blank fields"
      (is (= [] (:tags (supply-default-faq-fields {}))) ":tags is supplied"))
    (testing "doesn't override existing fields"
      (let [faq {:title "title" :body "body" :tags ["tag"]}
            updated-faq (supply-default-faq-fields faq)]
        (is (= (:title faq) (:title updated-faq)) ":title isn't changed")
        (is (= (:body faq) (:body updated-faq)) ":body isn't changed")
        (is (= (:tags faq) (:tags updated-faq)) ":tags isn't changed")))))
