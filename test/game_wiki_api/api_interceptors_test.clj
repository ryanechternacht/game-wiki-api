(ns game-wiki-api.api-interceptors-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.api-interceptors :refer :all]
            [io.pedestal.http.route :as route]))

(deftest attach-db-test
  (testing "Attach Db Test"
    (let [attach-db-fn (:enter attach-db)]
      (is attach-db-fn "attach-db interceptor has an enter fn")
      (let [before-context {:other "other val"}
            after-context (attach-db-fn before-context)]
        (is (get-in after-context [:request :database]) "database attached")
        (is (= (:other before-context) (:other after-context)) "other value preserved")))))

;; testing interceptors
(deftest echo-test
  (testing "Echo Test"
    (let [echo-fn (:enter echo)]
      (is echo-fn "echo interceptor has an enter fn")
      (let [before-context {:test "test-val"}
            after-context (echo-fn before-context)]
        (is (= before-context (get-in after-context [:response :body])) "echo context as response")
        (is (= (:test before-context) (:test after-context)) "context preserved")))))

(deftest echo-json-body-test
  (testing "Echo Json Body Test"
    (let [echo-json-body-fn (:enter echo-json-body)]
      (is echo-json-body-fn "echo-json-body has an enter fn")
      (let [before-context {:other "other-val"
                            :request {:json-params {:param-1 1 :param-2 2 :param-c "c"}}}
            after-context (echo-json-body-fn before-context)]
        (is (= (get-in [:request :json-params] before-context) (get-in [:response :body] after-context)) "json was returned as body")
        (is (= (:other before-context) (:other after-context)) "context is preserved")))))

;; card interceptors
(deftest get-cards-test
  (testing "Get Cards Test"
    (let [get-cards-fn (:enter get-cards)]
      (is get-cards-fn "get-cards has an enter fn")
      (let [cards [1 2 3]
            db-map {:read-cards (fn [] cards)}
            before-context {:request {:database db-map} :other-val "other-val"}
            after-context (get-cards-fn before-context)]
        (is (= cards (get-in after-context [:response :body])) "cards is body")
        (is (= (:other-val before-context) (:other-val after-context)) "context is preserved")))))

(deftest get-card-test
  (testing "Get Card Test"
    (let [get-card-fn (:enter get-card)
          card-id 1234
          card {:name "hello, world" :id card-id}
          db-map {:read-card-by-id (fn [i] (if (= i card-id) card nil))}]
      (is get-card-fn "get-card has an enter fn")
      (testing "Card Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:card-id (str card-id)}}
                              :other "other-val"}
              after-context (get-card-fn before-context)]
          (is (= card (get-in after-context [:response :body])) "card is returned as body")
          (is (= (:other before-context) (:other after-context)) "context is preserved")))
      (testing "Card Not Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:card-id (str (inc card-id))}}
                              :other "other-val"}
              after-context (get-card-fn before-context)]
          (is (= nil (get-in after-context [:response :body])) "not body si returned")
          (is (= (:other before-context) (:other after-context)) "context is preserved"))))))

; requires rebinding io.pedestal.http.route/*url-for*
(deftest post-card-test
  (testing "Post Card Test"
    (let [post-card-fn (:enter post-card)]
      (is post-card-fn "post-card has an enter fn")
      (binding [route/*url-for* (fn [_ _ _] "")]
        (let [card-data {:name "hello, world"}
              db-map {:create-card! (fn [card] (assoc card :id 1))}
              before-context {:other "other-val"
                              :request {:json-params card-data
                                        :database db-map}}
              after-context (post-card-fn before-context)]
          (is (= (:other before-context) (:other after-context)) "context is preserved")
          (is (= (:name card-data) (get-in after-context [:response :body :name])) "body has the new card"))))))

(deftest put-card-test
  (testing "Put Card Test"
    (let [put-card-fn (:enter put-card)]
      (is put-card-fn "put-card has an enter fn")
      (let [card-data {:name "hello, world" :id 2}
            db-map {:update-card! (fn [card] card)}
            before-context {:other "other-val"
                            :request {:json-params card-data
                                      :database db-map}}
            after-context (put-card-fn before-context)]
        (is (= card-data (get-in after-context [:response :body])) "body is the updated card")
        (is (= (:other before-context) (:other after-context)) "context is preserved")))))

;; faq interceptors
(deftest get-faq-test
  (testing "Get Faq Test"
    (let [get-faq-fn (:enter get-faq)
          faq-id 1234
          faq {:name "hello, world" :id faq-id}
          db-map {:read-faq-by-id (fn [i] (if (= i faq-id) faq nil))}]
      (is get-faq-fn "get-faq has an enter fn")
      (testing "Faq Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:faq-id (str faq-id)}}
                              :other "other-val"}
              after-context (get-faq-fn before-context)]
          (is (= faq (get-in after-context [:response :body] "faq is returned as body")))
          (is (= (:other before-context) (:other after-context)) "context is preserved")))
      (testing "Faq Not Found"
        (let [before-context {:request {:database db-map
                                        :path-params {:faq-id (str (inc faq-id))}}
                              :other "other-val"}
              after-context (get-faq-fn before-context)]
          (is (= nil (get-in after-context [:response :body])) "no body is returned")
          (is (= (:other before-context) (:other after-context)) "context is preserved"))))))

(deftest get-faqs-simple-test
  (testing "Get Faqs Simple Test"
    (let [get-faqs-simple-fn (:enter get-faqs-simple)]
      (is get-faqs-simple-fn "get-faqs-simple has an enter fn")
      (let [faqs {1 {} 2 {} 3 {}}
            db-map {:read-faqs-simple (fn [] faqs)}
            before-context {:request {:database db-map}
                            :other "other-val"}
            after-context (get-faqs-simple-fn before-context)]
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

(deftest get-popular-faq-tags-test
  (testing "List Popular Faq Tags Test"
    (let [get-popular-faq-tags-fn (:enter get-popular-faq-tags)]
      (is get-popular-faq-tags-fn "get-popular-faq-tags has an enter fn")
      (let [tags '("hello" "world")
            db-map {:get-popular-faq-tags (fn [] tags)}
            before-context {:request {:database db-map}
                            :other "other"}
            after-context (get-popular-faq-tags-fn before-context)]
        (is (= tags (get-in after-context [:response :body])) "tags are returned")
        (is (= (:other before-context) (:other after-context)) "context is preserved")))))

; requires rebinding io.pedestal.http.route/*url-for*
(deftest post-faq-test
  (testing "Post Faq Test"
    (let [post-faq-fn (:enter post-faq)]
      (is post-faq-fn "post-faq has an enter fn")
      (binding [route/*url-for* (fn [_ _ _] "")]
        (let [faq-data {:title "hello, world" :body "body" :tags []}
              db-map {:create-faq! (fn [faq] (assoc faq :id 1))}
              before-context {:other "other-val"
                              :request {:json-params faq-data
                                        :database db-map}}
              after-context (post-faq-fn before-context)]
          (is (= (:other before-context) (:other after-context)) "context is preserved")
          (is (= faq-data (dissoc (get-in after-context [:response :body]) :id)) "body has the new faq"))))))

(deftest put-faq-test
  (testing "Put Faq Test"
    (let [put-faq-fn (:enter put-faq)]
      (is put-faq-fn "put-faq has an enter fn")
      (let [faq-data {:title "hello, world" :body "body" :tags [] :id 1}
            db-map {:update-faq! (fn [faq] (assoc faq :id 1))}
            before-context {:other "other-val"
                            :request {:json-params faq-data
                                      :database db-map}}
            after-context (put-faq-fn before-context)]
        (is (= (:other before-context) (:other after-context)) "context is preserved")
        (is (= faq-data (get-in after-context [:response :body])) "body has the new faq")))))
