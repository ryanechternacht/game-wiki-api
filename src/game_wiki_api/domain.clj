(ns game-wiki-api.domain)

;; This probably needs a better name

(defn validate-new-card [card]
  (let [nm (:name card)]
    (not (nil? nm)))) ;; i add more things here eventually
