(ns game-wiki-api.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.test :as test]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [game-wiki-api.api-interceptors :as ints]))

;;; Routes
;; (def numeric #"[0-9]+")
;; (def url-rules {:card-id numeric :faq-id numeric})

(def routes
  (route/expand-routes
   #{["/cards" :get [ints/attach-db ints/get-cards]]
     ["/card/:card-id" :get [ints/attach-db ints/get-card]]; :constraints url-rules]
     ["/cards" :post [(body-params/body-params) ints/attach-db ints/post-put-card]]
     ["/faqs/popular-tags" :get [ints/attach-db ints/get-popular-faq-tags]]
     ["/faqs" :get [ints/attach-db ints/get-faqs-simple]]
     ["/faq/:faq-id" :get [ints/attach-db ints/get-faq]]; :constraints url-rules]
     ["/faqs/search/:faq-search" :get [ints/attach-db ints/search-faqs]]
     ["/faqs" :post [(body-params/body-params) ints/attach-db ints/post-put-faq]]}))
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
