(ns game-wiki-api.api-interceptors-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.api-interceptors :refer :all]
            [io.pedestal.http.route :as route]))

(deftest attach-db-test
  (testing "Attach Db Function Test"
    (let [attach-db-fn (:enter db-interceptor)]
      (is attach-db-fn "attach-db interceptor has an enter fn")
      (let [before-context {:other "other val"}
            after-context (attach-db-fn before-context)]
        (is (get-in after-context [:request :database]) "database attached")
        (is (= (:other before-context) (:other after-context)) "other value preserved")))))

;; testing interceptors
(deftest echo-test
  (testing "Echo Function Test"
    (let [echo-fn (:enter echo)]
      (is echo-fn "echo interceptor has an enter fn")
      (let [before-context {:test "test-val"}
            after-context (echo-fn before-context)]
        (is (= before-context (get-in after-context [:response :body])) "echo context as response")
        (is (= (:test before-context) (:test after-context)) "context preserved")))))

(deftest echo-json-body-test
  (testing "Echo Json Body Function Test"
    (let [echo-json-body-fn (:enter echo-json-body)]
      (is echo-json-body-fn "echo-json-body has an enter fn")
      (let [before-context {:other "other-val"
                            :request {:json-params {:param-1 1 :param-2 2 :param-c "c"}}}
            after-context (echo-json-body-fn before-context)]
        (is (= (get-in [:request :json-params] before-context) (get-in [:response :body] after-context)) "json was returned as body")
        (is (= (:other before-context) (:other after-context)) "context is preserved")))))

;; card interceptors
(deftest list-cards-test
  (testing "List Cards Function Test"
    (let [list-cards-fn (:enter list-cards)]
      (is list-cards-fn "list-cards has an enter fn")
      (let [cards [1 2 3]
            db-map {:read-cards (fn [] cards)}
            before-context {:request {:database db-map} :other-val "other-val"}
            after-context (list-cards-fn before-context)]
        (is (= cards (get-in after-context [:response :body])) "cards is body")
        (is (= (:other-val before-context) (:other-val after-context)) "context is preserved")))))

(deftest view-card-test
  (testing "View Card Function Test"
    (let [view-card-fn (:enter view-card)
          card-id 1234
          card {:name "hello, world" :id card-id}
          db-map {:read-card-by-id (fn [i] (if (= i card-id) card nil))}]
      (is view-card-fn "view-card has an enter fn")
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
(deftest create-update-card-test
  (testing "Put Card Function Test"
    (let [create-update-card-fn (:enter create-update-card)]
      (is create-update-card-fn "create-update-card has an enter fn")
      (binding [route/*url-for* (fn [_ _ _] "")]
        (let [card-data {:name "hello, world"}
              db-map {:save-card! (fn [card] (assoc card :id 1))}
              before-context {:other "other-val"
                              :request {:json-params card-data
                                        :database db-map}}
              after-context (create-update-card-fn before-context)]
          (is (= (:other before-context) (:other after-context)) "context is preserved")
          (is (= (:name card-data) (get-in after-context [:response :body :name])) "body has the new card"))))))

;; faq interceptors
(deftest view-faq-test
  (testing "View Faq Function Test"
    (let [view-faq-fn (:enter view-faq)
          faq-id 1234
          faq {:name "hello, world" :id faq-id}
          db-map {:read-faq-by-id (fn [i] (if (= i faq-id) faq nil))}]
      (is view-faq-fn "view-faq has an enter fn")
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
    (let [list-faqs-simple-fn (:enter list-faqs-simple)]
      (is list-faqs-simple-fn "list-faqs-simple has an enter fn")
      (let [faqs {1 {} 2 {} 3 {}}
            db-map {:read-faqs-simple (fn [] faqs)}
            before-context {:request {:database db-map}
                            :other "other-val"}
            after-context (list-faqs-simple-fn before-context)]
        (is (= faqs (get-in after-context [:response :body])) "faqs are returned as body")
        (is (= (:other before-context) (:other after-context)) "context is preserved")))))

(deftest search-faqs-test
  (testing "Search Faqs Test"
    (let [search-faqs-fn (:enter search-faqs)]
      (is search-faqs-fn "search-faqs has an enter fn")
      (let [faqs {1 {} 2 {} 3 {}}
            search-param "search"
            db-map {:search-faqs (fn [i] (if (= i search-param) faqs nil))}
            before-context {:request {:database db-map
                                      :path-params {:faq-search "search"}}
                            :other "other"}
            after-context (search-faqs-fn before-context)]
        (is (= faqs (get-in after-context [:response :body] "faqs are returned")))
        (is (= (:other before-context) (:other after-context)) "context is preserved")))))

(deftest list-popular-faq-tags-test
  (testing "List Popular Faq Tags Test"
    (let [list-popular-faq-tags-fn (:enter list-popular-faq-tags)]
      (is list-popular-faq-tags-fn "list-popular-faq-tags has an enter fn")
      (let [tags '("hello" "world")
            db-map {:get-popular-faq-tags (fn [] tags)}
            before-context {:request {:database db-map}
                            :other "other"}
            after-context (list-popular-faq-tags-fn before-context)]
        (is (= tags (get-in after-context [:response :body])) "tags are returned")
        (is (= (:other before-context) (:other after-context)) "context is preserved")))))

; requires rebinding io.pedestal.http.route/*url-for*
(deftest create-update-faq-test
  (testing "Put Faq Function Test"
    (let [create-update-faq-fn (:enter create-update-faq)]
      (is create-update-faq-fn "create-update-faq has an enter fn")
      (binding [route/*url-for* (fn [_ _ _] "")]
        (let [faq-data {:title "hello, world" :body "body" :tags []}
              db-map {:save-faq! (fn [faq] (assoc faq :id 1))}
              before-context {:other "other-val"
                              :request {:json-params faq-data
                                        :database db-map}}
              after-context (create-update-faq-fn before-context)]
          (is (= (:other before-context) (:other after-context)) "context is preserved")
          (is (= faq-data (dissoc (get-in after-context [:response :body]) :id)) "body has the new faq"))))))
