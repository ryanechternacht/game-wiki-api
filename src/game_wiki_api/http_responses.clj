(ns game-wiki-api.http-responses)

; wraps the supplied data in a {:data data} so it can
; be easily adorned by other interceptors
(defn simple-response [status data & {:as headers}]
  {:status status :body {:data data} :headers headers})

; expects the supplied body to already be wrapped.
; e.g. this is useful when you're generating paged results
; and already need to be applying some meta-data (page count/max)
(defn advanced-response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok-simple (partial simple-response 200))
(def created-simple (partial simple-response 201))
;; (def accepted (partial simple-response 202))

(def ok (partial advanced-response 200))
(def created (partial advanced-response 201))
;; (def accepted-advanced (partial simple-response 202))
(def invalid (partial advanced-response 400))
(def error (partial advanced-response 500))

