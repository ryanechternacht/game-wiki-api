(ns game-wiki-api.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.test :as test]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [game-wiki-api.api-interceptors :as api]))

;;; Routes
(def routes
  (route/expand-routes
   #{["/cards" :get [api/db-interceptor api/list-cards]]
     ["/card/:card-id" :get [api/db-interceptor api/view-card]]
     ["/cards" :post [(body-params/body-params) api/db-interceptor api/create-card]]
     ["/json" :post [(body-params/body-params) api/echo-json-body]]
     ["/faq/:faq-id" :get [api/db-interceptor api/view-faq]]}))

;;; Service
(def service-map
  {::http/routes routes
   ::http/type :jetty
   ::http/port 8890})

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
