(ns game-wiki-api.domain)

;; This probably needs a better name

(defn validate-new-card [card]
  (let [nm (:name card)]
    (not (nil? nm)))) ;; add more things here eventually

(defn validate-update-card [card]
  (and (:id card) (validate-new-card card)))

(defn supply-default-faq-fields [faq]
  (assoc faq :tags (:tags faq [])))

(defn validate-new-faq [faq]
  (and (not (nil? (:title faq)))
       (not (nil? (:body faq)))
       (not (nil? (:tags faq)))))
