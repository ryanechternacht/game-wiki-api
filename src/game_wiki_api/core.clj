(ns game-wiki-api.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.test :as test]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [game-wiki-api.api-interceptors :as ints]))

;;; Routes
(def numeric #"[0-9]+")
; I can't get a shared constraints map for some reason

(def routes
  (route/expand-routes
   #{["/cards" :get [ints/attach-db ints/get-cards]]
     ["/cards" :post [(body-params/body-params) ints/attach-db ints/post-card]]
     ["/card/:card-id" :get [ints/attach-db ints/get-card] :constraints {:card-id numeric}]
     ["/card/:card-id" :put [(body-params/body-params) ints/attach-db ints/put-card] :constraints {:card-id numeric}]
     ["/faqs/popular-tags" :get [ints/attach-db ints/get-popular-faq-tags]]
     ["/faqs" :get [ints/attach-db ints/get-faqs-simple]]
     ["/faqs" :post [(body-params/body-params) ints/attach-db ints/post-faq]]
     ["/faq/:faq-id" :get [ints/attach-db ints/get-faq] :constraints {:faq-id numeric}]
     ["/faq/:faq-id" :put [(body-params/body-params) ints/attach-db ints/put-faq]]
     ["/faqs/search/:faq-search" :get [ints/attach-db ints/search-faqs]]}))
     ;; testing
     ;; ["/json" :post [(body-params/body-params) ints/echo-json-body]]

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
