(ns game-wiki-api.api-interceptors-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.api-interceptors :refer :all]
            [io.pedestal.http.route :as route]))

(deftest attach-db-fn-test
  (testing "Attach Db Function Test"
    (let [other-val "other val"
          after-context (attach-db-fn {:other other-val})]
      (is (get-in after-context [:request :database]) "database attached")
      (is (= (:other after-context) other-val) "other value preserved"))))

;; testing interceptors
(deftest echo-fn-test
  (testing "Echo Function Test"
    (let [before-context {:test "test-val"}
          after-context (echo-fn before-context)]
      (is (= before-context (get-in after-context [:response :body])) "echo context as response")
      (is (= (:test before-context) (:test after-context)) "context preserved"))))

(deftest echo-json-body-test
  (testing "Echo Json Body Function Test"
    (let [before-context {:other "other-val"
                          :request {:json-params {:param-1 1 :param-2 2 :param-c "c"}}}
          after-context (echo-json-body-fn before-context)]
      (is (= (get-in [:request :json-params] before-context) (get-in [:response :body] after-context)) "json was returned as body")
      (is (= (:other before-context) (:other after-context)) "context is preserved"))))

;; card interceptors
(deftest list-cards-fn-test
  (testing "List Cards Function Test"
    (let [cards [1 2 3]
          db-map {:read-cards (fn [] cards)}
          before-context {:request {:database db-map} :other-val "other-val"}
          after-context (list-cards-fn before-context)]
      (is (= cards (get-in after-context [:response :body])) "cards is body")
      (is (= (:other-val before-context) (:other-val after-context)) "context is preserved"))))

(deftest view-card-fn-test
  (testing "View Card Function Test"
    (let [card-id 1234
          card {:name "hello, world" :id card-id}
          db-map {:read-card-by-id (fn [i] (if (= i card-id) card nil))}]
      (testing "Card Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:card-id (str card-id)}}
                              :other "other-val"}
              after-context (view-card-fn before-context)]
          (is (= card (get-in after-context [:response :body])) "card is returned as body")
          (is (= (:other before-context) (:other after-context)) "context is preserved")))
      (testing "Card Not Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:card-id (str (inc card-id))}}
                              :other "other-val"}
              after-context (view-card-fn before-context)]
          (is (= nil (get-in after-context [:response :body])) "not body si returned")
          (is (= (:other before-context) (:other after-context)) "context is preserved"))))))

; requires rebinding io.pedestal.http.route/*url-for*
(deftest create-card-fn-test
  (testing "Create Card Function Test"
    (binding [route/*url-for* (fn [a b c] "")]
      (let [card-data {:name "hello, world"}
            db-map {:save-card! (fn [card] (assoc card :id 1))}
            before-context {:other "other-val"
                            :request {:json-params card-data
                                      :database db-map}}
            after-context (create-card-fn before-context)]
        (is (= (:other before-context) (:other after-context)) "context is preserved")
        (is (= (:name card-data) (get-in after-context [:response :body :name])) "body has the new card")))))

;; faq interceptors
(deftest view-faq-fn-test
  (testing "View Faq Function Test"
    (let [faq-id 1234
          faq {:name "hello, world" :id faq-id}
          db-map {:read-faq-by-id (fn [i] (if (= i faq-id) faq nil))}]
      (testing "Faq Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:faq-id (str faq-id)}}
                              :other "other-val"}
              after-context (view-faq-fn before-context)]
          (is (= faq (get-in after-context [:response :body] "faq is returned as body")))
          (is (= (:other before-context) (:other after-context)) "context is preserved")))
      (testing "Faq Not Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:faq-id (str (inc faq-id))}}
                              :other "other-val"}
              after-context (view-faq-fn before-context)]
          (is (= nil (get-in after-context [:response :body])) "no body is returned")
          (is (= (:other before-context) (:other after-context)) "context is preserved"))))))

(deftest list-faqs-simple-test
  (testing "View Faqs Simple Test"
    (let [faqs {1 {} 2 {} 3 {}}
          db-map {:read-faqs-simple (fn [] faqs)}
          before-context {:request {:database db-map}
                          :other "other-val"}
          after-context (list-faqs-simple-fn before-context)]
      (is (= faqs (get-in after-context [:response :body])) "faqs are returned as body")
      (is (= (:other before-context) (:other after-context)) "context is preserved"))))

(deftest search-faqs-test
  (testing "Search Faqs Test"
    (let [faqs {1 {} 2 {} 3 {}}
          search-param "search"
          db-map {:search-faqs (fn [i] (if (= i search-param) faqs nil))}
          before-context {:request {:database db-map
                                    :path-params {:faq-search "search"}}
                          :other "other"}
          after-context (search-faqs-fn before-context)]
      (is (= faqs (get-in after-context [:response :body] "faqs are returned")))
      (is (= (:other before-context) (:other after-context)) "context is preserved"))))

(deftest list-popular-faq-tags-test
  (testing "List Popular Faq Tags Test"
    (let [tags '("hello" "world")
          db-map {:get-popular-faq-tags (fn [] (prn "hello") tags)}
          before-context {:request {:database db-map}
                          :other "other"}
          after-context (list-popular-faq-tags-fn before-context)]
      (is (= tags (get-in after-context [:response :body])) "tags are returned")
      (is (= (:other before-context) (:other after-context)) "context is preserved"))))
