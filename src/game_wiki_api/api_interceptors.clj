(ns game-wiki-api.api-interceptors
  (:require [game-wiki-api.database.in-memory-database :as db]
            [game-wiki-api.http-responses :as resp]
            [game-wiki-api.domain :as domain]
            [clojure.edn :as edn]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.content-negotiation :as con-neg]
            [cheshire.core :as json]))

;; TODO "common interceptors" - conn-eg, renderer, db
;; common interceptors
(def db-map (db/get-db-map))

(def attach-db
  {:name :attach-db
   :enter
   (fn [context]
     (assoc-in context [:request :database] db-map))})

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(def content-negotiation (con-neg/negotiate-content supported-types))

(def render-result
  {:name :render-result
   :leave
   (fn [context]
     (let [accepted (get-in context [:request :accept :field] "text/html")]
       (let [response (:response context)
             body (:body response)
             headers (:headers response {})
             rendered-body (case accepted
                             "text/html" body
                             "text/plain" body
                             "application-edn" (pr-str body)
                             "application/json" (json/encode body)
                             body)

             updated-response (assoc response
                                     :headers (assoc headers "Content-Type" accepted)
                                     :body rendered-body)]
         (assoc context :response updated-response))))})

;; testing interceptors
(def print-request
  {:name :print-request
   :enter
   (fn [context]
     (let [request (:request context)]
       (prn request)
       request))})

(def echo
  {:name :echo
   :enter
   (fn [context]
     (let [response (resp/ok context)]
       (assoc context :response response)))})

(def echo-json-body
  {:name :echo-json-body
   :enter
   (fn [context]
     (let [request (:request context)
           json (:json-params request)]
       (assoc context :response (resp/ok json))))})

;; card interceptors
(def get-cards
  {:name :get-cards
   :enter
   (fn [context]
     (prn (:request context))
     (let [read-cards (get-in context [:request :database :read-cards])
           cards (read-cards)
           response (resp/ok cards)]
       (assoc context :response response)))})

(def get-card
  {:name :get-card
   :enter
   (fn [context]
     (let [read-card-by-id (get-in context [:request :database :read-card-by-id])]
       (if-let [card-id (edn/read-string (get-in context [:request :path-params :card-id]))]
         (if-let [the-card (read-card-by-id card-id)]
           (let [response (resp/ok the-card)]
             (assoc context :response response))
           context)
         context)))})

(def post-card
  {:name :post-card
   :enter
   (fn [context]
     (let [card-data (get-in context [:request :json-params])
           create-card! (get-in context [:request :database :create-card!])]
       (if (domain/validate-new-card card-data)
         (let [new-card (create-card! card-data)
               url (route/url-for :get-card :params {:card-id (:id new-card)})]
           (assoc context
                  :response (resp/created new-card "Location" url)))
         (assoc context :response (resp/invalid {:error "Invalid card data supplied"})))))})

(def put-card
  {:name :put-card
   :enter
   (fn [context]
     (let [card-data (get-in context [:request :json-params])
           update-card! (get-in context [:request :database :update-card!])]
       (if (domain/validate-update-card card-data)
         (let [updated-card (update-card! card-data)]
           (assoc context :response (resp/ok updated-card)))
         (assoc context :response (resp/invalid {:error "Invalid card data supplied"})))))})

;; faq interceptor
(def get-faq
  {:name :get-faq
   :enter
   (fn [context]
     (let [read-faq-by-id (get-in context [:request :database :read-faq-by-id])]
       (if-let [faq-id (edn/read-string (get-in context [:request :path-params :faq-id]))]
         (if-let [the-faq (read-faq-by-id faq-id)]
           (assoc context :response (resp/ok the-faq))
           context)
         context)))})

(def get-faqs-simple
  {:name :get-faqs-simple
   :enter
   (fn [context]
     (let [read-faqs-simple (get-in context [:request :database :read-faqs-simple])]
       (assoc context :response (resp/ok (read-faqs-simple)))))})

(def search-faqs
  {:name :search-faqs
   :enter
   (fn [context]
     (let [search-faqs (get-in context [:request :database :search-faqs])
           search-query (get-in context [:request :path-params :faq-search])]
       (assoc context :response (resp/ok (search-faqs search-query)))))})

(def get-popular-faq-tags
  {:name :get-popular-faq-tags
   :enter
   (fn [context]
     (let [gpft (get-in context [:request :database :get-popular-faq-tags])]
       (assoc context :response (resp/ok (gpft)))))})

(def post-faq
  {:name :post-faq
   :enter
   (fn [context]
     (let [faq-data (domain/supply-default-faq-fields (get-in context [:request :json-params]))
           create-faq! (get-in context [:request :database :create-faq!])]
       (if (domain/validate-new-faq faq-data)
         (let [new-faq (create-faq! faq-data)
               url (route/url-for :get-faq :params {:faq-id (:id new-faq)})]
           (assoc context :response
                  (resp/created new-faq "Location" url)))
         (assoc context :response (resp/invalid {:error "Faq requires body and title"})))))})

(def put-faq
  {:name :put-faq
   :enter
   (fn [context]
     (let [faq-data (domain/supply-default-faq-fields (get-in context [:request :json-params]))
           update-faq! (get-in context [:request :database :update-faq!])]
       (if (domain/validate-update-faq faq-data)
         (let [updated-faq (update-faq! faq-data)]
           (assoc context :response (resp/ok updated-faq)))
         (assoc context :response (resp/invalid {:error "Invalid faq data supplied"})))))})
