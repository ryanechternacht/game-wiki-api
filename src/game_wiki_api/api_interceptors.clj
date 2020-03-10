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

(defn create-card-fn [context]
  (let [card-data (get-in context [:request :json-params])
        save-card! (get-in context [:request :database :save-card!])]
    (if (domain/validate-new-card card-data)
      (let [new-card (save-card! card-data)
            url (route/url-for :view-card :params {:card-id (:id new-card)})]
        (assoc context
               :response (resp/created new-card "Location" url)))
      (assoc context :response (resp/invalid {:error "Card data not properly supplied"})))))

(def create-card
  {:name :create-card
   :enter create-card-fn})

(defn echo-json-body-fn [context]
  (let [request (:request context)
        json (:json-params request)]
    (assoc context :response (resp/ok json))))

(def echo-json-body
  {:name :echo-json-body
   :enter echo-json-body-fn})
