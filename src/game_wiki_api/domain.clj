(ns game-wiki-api.domain)

;; This probably needs a better name

(defn validate-card [card]
  (let [id (:id card)
        nm (:name card)]
    (not (nil? (and id nm)))))

(defn get-next-card-id [db]
  (inc
   (reduce max (keys (:cards db)))))

(defn make-card [db card]
  (assoc card :id (get-next-card-id db)))
