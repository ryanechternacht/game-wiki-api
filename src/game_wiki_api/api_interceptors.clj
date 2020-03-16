(ns game-wiki-api.api-interceptors
  (:require [game-wiki-api.database.in-memory-database :as db]
            [game-wiki-api.http-responses :as resp]
            [game-wiki-api.domain :as domain]
            [clojure.edn :as edn]
            [io.pedestal.http.route :as route]))

(def db-map (db/get-db-map))

(defn attach-db-fn [context]
  (assoc-in context [:request :database] db-map))

(def db-interceptor
  {:name :db-interceptor
   :enter attach-db-fn})

;; testing interceptors
(defn print-request-fn [context]
  (let [request (:request context)]
    (print request)
    request))

(def print-request
  {:name :print-request
   :enter print-request-fn})

(defn echo-fn [context]
  (let [response (resp/ok context)]
    (assoc context :response response)))

(def echo
  {:name :echo
   :enter echo-fn})

(defn echo-json-body-fn [context]
  (let [request (:request context)
        json (:json-params request)]
    (assoc context :response (resp/ok json))))

(def echo-json-body
  {:name :echo-json-body
   :enter echo-json-body-fn})

;; card interceptors
(defn list-cards-fn [context]
  (let [read-cards (get-in context [:request :database :read-cards])
        cards (read-cards)
        response (resp/ok cards)]
    (assoc context :response response)))

(def list-cards
  {:name :list-cards
   :enter list-cards-fn})

(defn view-card-fn [context]
  (let [read-card-by-id (get-in context [:request :database :read-card-by-id])]
    (if-let [card-id (edn/read-string (get-in context [:request :path-params :card-id]))]
      (if-let [the-card (read-card-by-id card-id)]
        (let [response (resp/ok the-card)]
          (assoc context :response response))
        context)
      context)))

(def view-card
  {:name :view-card
   :enter view-card-fn})

(defn create-update-card-fn [context]
  (let [card-data (get-in context [:request :json-params])
        save-card! (get-in context [:request :database :save-card!])]
    (if (domain/validate-new-card card-data)
      (let [new-card (save-card! card-data)
            url (route/url-for :view-card :params {:card-id (:id new-card)})]
        (assoc context
               :response (resp/created new-card "Location" url)))
      (assoc context :response (resp/invalid {:error "Card data not properly supplied"})))))

(def create-update-card
  {:name :create-update-card
   :enter create-update-card-fn})

;; faq interceptor
(defn view-faq-fn [context]
  (let [read-faq-by-id (get-in context [:request :database :read-faq-by-id])]
    (if-let [faq-id (edn/read-string (get-in context [:request :path-params :faq-id]))]
      (if-let [the-faq (read-faq-by-id faq-id)]
        (assoc context :response (resp/ok the-faq))
        context)
      context)))

(def view-faq
  {:name :view-faq
   :enter view-faq-fn})

(defn list-faqs-simple-fn [context]
  (let [read-faqs-simple (get-in context [:request :database :read-faqs-simple])]
    (assoc context :response (resp/ok (read-faqs-simple)))))

(def list-faqs-simple
  {:name :list-faqs-simple
   :enter list-faqs-simple-fn})

(defn search-faqs-fn [context]
  (let [search-faqs (get-in context [:request :database :search-faqs])
        search-query (get-in context [:request :path-params :faq-search])]
    (assoc context :response (resp/ok (search-faqs search-query)))))

(def search-faqs
  {:name :search-faqs
   :enter search-faqs-fn})

(defn list-popular-faq-tags-fn [context]
  (let [get-popular-faq-tags (get-in context [:request :database :get-popular-faq-tags])]
    (assoc context :response (resp/ok (get-popular-faq-tags)))))

(def list-popular-faq-tags
  {:name :list-popular-faq-tags
   :enter list-popular-faq-tags-fn})

(defn create-update-faq-fn [context]
  (prn (:request context))
  (let [faq-data (domain/supply-default-faq-fields (get-in context [:request :json-params]))
        save-faq! (get-in context [:request :database :save-faq!])]
    (if (domain/validate-new-faq faq-data)
      (let [new-faq (save-faq! faq-data)
            url (route/url-for :view-faq :params {:faq-id (:id new-faq)})]
        (assoc context :response
               (resp/created new-faq "Location" url)))
      (assoc context :response (resp/invalid {:error "Faq requires body and title"})))))

(def create-update-faq
  {:name :create-update-faq
   :enter create-update-faq-fn})
