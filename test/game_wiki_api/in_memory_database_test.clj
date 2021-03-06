(ns game-wiki-api.in-memory-database-test
  (:require [clojure.test :refer :all]
            [game-wiki-api.database.in-memory-database :refer :all]))

;; cards test
(deftest read-cards-test
  (testing "Read Cards Test"
    (let [db-val {:cards {1 {:id 1 :name "card 1"} 2 {:id 2 :name "card 2"}}}
          funcs (get-db-map (atom db-val))
          read-cards (:read-cards funcs)]
      (is read-cards "read-cards is defined")
      (let [cards (read-cards)]
        (is (= 2 (count cards)) "Has correct key count")
        (is (= "card 1" (:name (first cards))) "Card 1 has the correct name")))))

(deftest read-card-by-id-test
  (testing "Read Card By ID Test"
    (let [db-val {:cards {1 {:id 1 :name "card 1"} 2 {:id 2 :name "card 2"}}}
          funcs (get-db-map (atom db-val))
          read-card-by-id (:read-card-by-id funcs)]
      (is read-card-by-id "read-card-by-id is defined")
      (testing "Card Found"
        (let [card (read-card-by-id 1)]
          (is (= "card 1" (:name card)) "Card has the correct name")))
      (testing "Card Not Found"
        (let [no-card (read-card-by-id 3)]
          (is (= nil no-card) "no card returns nil"))))))

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

(deftest create-card!-test
  (testing "Create Card Test"
    (let [db-val {:cards {1 {:id 1 :name "card 1"} 2 {:id 2 :name "card 2"}}}
          db (atom db-val)
          funcs (get-db-map db)
          create-card! (:create-card! funcs)]
      (is create-card! "save-card is defined")
      (let [new-card {:name "card 3"}
            pre-cards-count (count (:cards @db))]
        (do
          (let [returned-card (create-card! new-card)]
            (is (= new-card (dissoc returned-card :id)) "create-card! returns the new card")
            (is (= (:name new-card) (get-in @db [:cards 3 :name])) "Card was added with the correct name")
            (is (= (inc pre-cards-count) (count (:cards @db))) "New card count is corectly")))))))

(deftest update-card!-test
  (testing "Update Card Test"
    (let [db-val {:cards {1 {:id 1 :name "card 1"} 2 {:id 2 :name "card 2"}}}
          db (atom db-val)
          new-name "update card 1"
          funcs (get-db-map db)
          update-card! (:update-card! funcs)]
      (is update-card! "update-card! is defined")
      (let [card {:id 1 :name new-name}
            pre-cards-count (count (:cards @db))]
        (do
          (let [returned-card (update-card! card)]
            (is (= card returned-card) "update-card! return and card are the same")
            (is (= pre-cards-count (count (:cards @db))) "Card count wasn't changed")
            (is (= new-name (get-in @db [:cards 1 :name])) "Card name was changed")))))))

;; faq tests
(deftest read-faq-by-id-test
  (testing "Read Faq by ID Test"
    (let [faq-id 1234
          faq {:id faq-id :title "Hello, World"}
          db-val {:faqs {1 {:id 1 :title ""} faq-id faq}}
          read-faq-by-id (:read-faq-by-id (get-db-map (atom db-val)))]
      (is read-faq-by-id "read-faq-by-id is defined")
      (testing "Card Found"
        (is (= faq (read-faq-by-id faq-id)) "faq is properly returned"))
      (testing "Card Not Found"
        (is (= nil (read-faq-by-id 3)) "nil is returned when faq is not found")))))

(deftest read-faqs-simple-test
  (testing "Read Faqs Simple Test"
    (let [db-val {:faqs {1 {:id 1 :title "hello" :body "other" :tags []} 2 {:id 2 :title "hello 2" :body "other 2" :tags ["hello"]}}}
          read-faqs-simple (:read-faqs-simple (get-db-map (atom db-val)))]
      (is read-faqs-simple "read-faqs-simple is defined")
      (let [faqs (read-faqs-simple)
            faq1 (first faqs)]
        (is (= (count (:faqs db-val)) (count faqs)) "correct count was returned")
        (is (and (:id faq1) (:title faq1)) "title and id are returned")
        (is (and (nil? (:body faq1)) (nil? (:tags faq1))) "body and tags aren't returned")))))

(deftest search-faqs-test
  (testing "Search Faqs Test"
    (let [db-val {:faqs {1 {:id 1 :title "hello"} 2 {:id 2 :body "world"} 3 {:id 3 :tags ["goodbye"]}}}
          search-faqs (:search-faqs (get-db-map (atom db-val)))]
      (is search-faqs "search-faqs is defined")
      (let [faqs (search-faqs "hello")
            faq1 (first faqs)]
        (is (= 1 (count faqs)) "title searching is correct")
        (is (and (:id faq1) (:title faq1)) "id and title are returned")
        (is (and (nil? (:body faq1)) (nil? (:tags faq1)) "body and tags aren't returned")))
      (is (= 1 (count (search-faqs "world"))) "search by body works")
      (is (= 1 (count (search-faqs "goodbye"))) "search by tags works"))))

(deftest get-popular-faq-tags-test
  (testing "Get Popular Faqs Tags Test"
    (let [db-val {:faqs {1 {:tags [1 2 3 4 5]}
                         2 {:tags [1 2 3 4 5]}
                         3 {:tags [1 2 3 4 5]}
                         4 {:tags [1 6]}}}
          get-popular-faq-tags (:get-popular-faq-tags (get-db-map (atom db-val)))]
      (is get-popular-faq-tags "get-popular-faq-tags is defined")
      (let [tags (get-popular-faq-tags)]
        (is (<= 4 (count tags)) "no more than 4 results are returned")
        (is (some #(= 1 %) tags) "contains 1, the key with the most")
        (is (every? #(not= 6 %) tags) "doesn't contain 6, the key with the least")))))

(deftest get-next-faq-id-test
  (testing "Get Next Faq ID Test"
    (testing "simple case"
      (is (= 2 (get-next-faq-id {:faqs {1 {}}}))))
    (testing "advanced case"
      (is (= 4 (get-next-faq-id {:faqs {1 {} 2 {} 3 {}}}))))
    (testing "missing ids"
      (is (= 11 (get-next-faq-id {:faqs {10 {}}}))))
    (testing "empty throws error"
      (is (thrown? Exception (get-next-faq-id {}))))))

(deftest create-faq!-test
  (testing "Create Faq Test"
    (let [db-val {:faqs {1 {:id 1 :title "faq 1"} 2 {:id 2 :title "faq 2"}}}
          db (atom db-val)
          create-faq! (:create-faq! (get-db-map db))]
      (is create-faq! "create-faq! is defined")
      (let [new-faq {:title "faq 3" :body "body 3"}
            pre-create-count (count (:faqs @db))]
        (do
          (let [returned-faq (create-faq! new-faq)]
            (is (= new-faq (dissoc returned-faq :id)) "create-faq! returns the new card")
            (is (= (:title new-faq) (get-in @db [:faqs 3 :title])) "new faq (title) was saved correctly")
            (is (= (:body new-faq) (get-in @db [:faqs 3 :body])) "new faq (body) was saved correctly")
            (is (= (inc pre-create-count) (count (:faqs @db))) "faq count is updated")))))))

(deftest update-faq!-test
  (testing "Update Faq Test"
    (let [db-val {:faqs {1 {:id 1 :title "faq 1"} 2 {:id 2 :title "faq 2"}}}
          db (atom db-val)
          update-faq! (:update-faq! (get-db-map db))]
      (is update-faq! "update-faq! is defined")
      (let [updated-faq {:id 1 :title "updated faq 1" :body "new body 1"}
            pre-update-count (count (:faqs @db))]
        (do
          (let [returned-faq (update-faq! updated-faq)]
            (is (= updated-faq returned-faq) "update-faq! returns the updated faq")
            (is (= pre-update-count (count (:faqs @db))) "faq count wasn't changed")
            (is (= (:title updated-faq) (get-in @db [:faqs 1 :title])) "faq title was changed")
            (is (= (:body updated-faq) (get-in @db [:faqs 1 :body])) "faq body was changed")))))))
