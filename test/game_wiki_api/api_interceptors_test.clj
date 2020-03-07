(ns game-wiki-api.api-interceptors-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.api-interceptors :refer :all]))

(deftest attach-db-fn-test
  (testing "Attach Db Function Test"
    (let [other-val "other val"
          after-context (attach-db-fn {:other other-val})]
      (is (get-in after-context [:request :database]) "database attached")
      (is (= (:other after-context) other-val) "other value preserved"))))

;; Can't test this until we inject the db
;; (deftest commit-transaction-fn-test
;;   (testing "Commit Transaction Function Test"
;;     (let [other-val "other val"
;;           test-val "test val"
;;           db {:other other-val}
;;           before-context {:tx-data [assoc :test test-val]}
;;           after-contxt (commit-transaction-fn before-context)]
;;       (is ()))))

(deftest echo-fn-test
  (testing "Echo Function Test"
    (let [before-context {:test "test-val"}
          after-context (echo-fn before-context)]
      (is (= before-context (get-in after-context [:response :body])) "echo context as response")
      (is (= (:test before-context) (:test after-context)) "context preserved"))))

(deftest list-cards-fn-test
  (testing "List Cards Function Test"
    (let [cards [1 2 3]
          before-context {:request {:database {:cards cards}} :other-val "other-val"}
          after-context (list-cards-fn before-context)]
      (is (= cards (get-in after-context [:response :body])) "cards is body")
      (is (= (:other-val before-context) (:other-val after-context)) "context is preserved"))))

(deftest view-card-fn-test
  (testing "View Card Function Test"
    (let [card-id 1234
          card {:name "hello, world" :id card-id}
          before-context {:request {:database {:cards {card-id card}}
                                    :path-params {:card-id (str card-id)}}
                          :other "other-val"}
          after-context (view-card-fn before-context)]
      (is (= card (get-in after-context [:response :body])) "card is returned as body")
      (is (= (:other before-context) (:other after-context)) "context is preserved"))))

; relying on underlying game-wiki-api.domain/make-card
; and url/for
;; (deftest create-card-fn-test
;;   (testing "Create Card Function Test"
;;     (let [card-data {:name "hello, world"}
;;           before-context {:other "other-val"
;;                           :request {:json-params card-data
;;                                     :database {:cards {1 {:id 1}}}}}
;;           after-context (create-card-fn before-context)
;;           tx-data (:tx-data after-context)]
;;       (is tx-data "tx-data was attached")
;;       (is (= (get 0 tx-data) assoc-in) "first element is assoc-in")
;;       (is (= (get-in [2 :name] tx-data) (:name card-data)) "new cards has name")
;;       (is (= (:other before-context) (:other after-context)) "context is preserved"))))

(deftest echo-json-body-test
  (testing "Echo Json Body Function Test"
    (let [before-context {:other "other-val"
                          :request {:json-params {:param-1 1 :param-2 2 :param-c "c"}}}
          after-context (echo-json-body-fn before-context)]
      (is (= (get-in [:request :json-params] before-context) (get-in [:response :body] after-context)) "json was returned as body")
      (is (= (:other before-context) (:other after-context)) "context is preserved"))))
