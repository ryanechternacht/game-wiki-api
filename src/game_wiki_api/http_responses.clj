(ns game-wiki-api.http-responses)

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))
(def invalid (partial response 400))
(def error (partial response 500))