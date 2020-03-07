(ns game-wiki-api.api-interceptors
  (:require [game-wiki-api.database.in-memory-database :as db]
            [game-wiki-api.http-responses :as resp]
            [game-wiki-api.domain :as domain]
            [clojure.edn :as edn]
            [io.pedestal.http.route :as route]))

;; TODO break up interceptor from underlying functions?
(defn attach-db-fn [context]
  (assoc-in context [:request :database] @db/database))

(defn commit-transaction-fn [context]
  (if-let [[op & args] (:tx-data context)]
    (do
      (apply swap! db/database op args)
      (assoc-in context [:request :database] @db/database))
    context))

(def db-interceptor
  {:name :db-interceptor
   :enter attach-db-fn
   :leave commit-transaction-fn})

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

(defn list-cards-fn [context]
  (let [db (get-in context [:request :database])
        cards (db/read-cards db)
        response (resp/ok cards)]
    (assoc context :response response)))

(def list-cards
  {:name :list-cards
   :enter list-cards-fn})

(defn view-card-fn [context]
  (let [db (get-in context [:request :database])]
    (if-let [card-id (edn/read-string (get-in context [:request :path-params :card-id]))]
      (if-let [the-card (db/read-card-by-id db card-id)]
        (let [response (resp/ok the-card)]
          (assoc context :response response))
        context)
      context)))

(def view-card
  {:name :view-card
   :enter view-card-fn})

(defn create-card-fn [context]
  (let [card-data (get-in context [:request :json-params])
        db (get-in context [:request :database])]
    (if-let [new-card (domain/make-card db card-data)]
      (if (domain/validate-card new-card)
        (let [new-id (:id new-card)
              url (route/url-for :view-card :params {:card-id new-id})]
          (assoc context
                 :response (resp/created new-card "Location" url)
                 :tx-data [assoc-in [:cards new-id] new-card]))
        (assoc context :response (resp/invalid {:error "Name not supplied"}))))))

(def create-card
  {:name :make-card
   :enter create-card-fn})

(defn echo-json-body-fn [context]
  (let [request (:request context)
        json (:json-params request)]
    (assoc context :response (resp/ok json))))

(def echo-json-body
  {:name :echo-json-body
   :enter echo-json-body-fn})
