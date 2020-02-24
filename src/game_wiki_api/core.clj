(ns game-wiki-api.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [clojure.edn :as edn]
            [cheshire.core :as json]))

;;; api helpers
(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))
(def invalid (partial response 400))
(def error (partial response 500))

;;; Domain functions

(defn validate-card [card]
  (let [id (:id card)
        nm (:name card)]
    (not (nil? (and id nm)))))

(defn get-next-card-id [db]
  (inc
   (reduce max (keys (:cards db)))))

(defn make-card [db card]
  (assoc card :id (get-next-card-id db)))

;;; Database functions
(defonce database (atom {:cards {1 {:name "card 1" :id 1} 2 {:name "card 2" :id 2}}}))

(defn read-cards [db]
  (:cards db))

(defn read-card-by-id [db id]
  (get-in db [:cards id]))

(defn attach-db-fn [context]
  (assoc-in context [:request :database] @database))

(defn commit-transaction-fn [context]
  (if-let [[op & args] (:tx-data context)]
    (do
      (apply swap! database op args)
      (assoc-in context [:request :database] @database))
    context))

(def db-interceptor
  {:name :db-interceptor
   :enter attach-db-fn
   :leave commit-transaction-fn})

;;; API Interceptors
(defn print-request-fn [context]
  (let [request (:request context)]
    (print request)
    request))

(def print-request
  {:name :print-request
   :enter print-request-fn})

(defn echo-fn [context]
  (let [response (ok context)]
    (assoc context :response response)))

(def echo
  {:name :echo
   :enter echo-fn})

(defn list-cards-fn [context]
  (let [db (get-in context [:request :database])
        cards (read-cards db)
        response (ok cards)]
    (assoc context :response response)))

(def list-cards
  {:name :list-cards
   :enter list-cards-fn})

(defn view-card-fn [context]
  (let [db (get-in context [:request :database])]
    (if-let [card-id (edn/read-string (get-in context [:request :path-params :card-id]))]
      (if-let [the-card (read-card-by-id db card-id)]
        (let [response (ok the-card)]
          (assoc context :response response))
        context)
      context)))

(def view-card
  {:name :view-card
   :enter view-card-fn})

(defn create-card-fn [context]
  (let [card-data (get-in context [:request :json-params])
        db (get-in context [:request :database])]
    (if-let [new-card (make-card db card-data)]
      (if (validate-card new-card)
        (let [new-id (:id new-card)
              url (route/url-for :view-card :params {:card-id new-id})]
          (assoc context
                 :response (created new-card "Location" url)
                 :tx-data [assoc-in [:cards new-id] new-card]))
        (assoc context :response (invalid {:error "Name not supplied"}))))))

(def create-card
  {:name :make-card
   :enter create-card-fn})

(defn echo-json-body-fn [context]
  (let [request (:request context)
        json (:json-params request)]
    (assoc context :response (ok json))))

(def echo-json-body
  {:name :echo-json-body
   :enter echo-json-body-fn})

;;; Routes
(def routes
  (route/expand-routes
   #{["/cards" :get [db-interceptor list-cards]]
     ["/card/:card-id" :get [db-interceptor view-card]]
     ["/cards" :post [(body-params/body-params) db-interceptor create-card]]
     ["/json" :post [(body-params/body-params) echo-json-body]]}))

;;; Service
(def service-map
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   8890})

(defonce server (atom nil))

(defn start []
  (reset! server
          (http/start
           (http/create-server
            (assoc service-map
                   ::http/join? false)))))

(defn stop []
  (http/stop @server))

(defn restart []
  (stop)
  (start))

; Testing
(defn test-request [verb url]
  (test/response-for (::http/service-fn @server) verb url))

(defn test-json-request [verb url body]
  (test/response-for
   (::http/service-fn @server)
   verb
   url
   :headers {"Content-Type" "application/json"}
   :body (json/encode body)))

(test-json-request "post" "/json" {:hello "world"})
