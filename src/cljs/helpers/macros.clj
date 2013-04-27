(ns helpers.macros)

;; (defmacro with-auth [user & body]
;;   (let [lst (last body)
;;         lst? (and (list? lst)
;;                   (= (first lst) :no-auth))
;;         lst* (if lst? (rest lst))
;;         ops (if lst? (butlast body) body)]
;;   `(if-not (or (nil? ~user) (empty? ~user))
;;      (do ~@ops)
;;      (do ~@lst*))))


(defmacro with-user [user {:keys [auth no-auth]}]
  `(if-not (or (nil? ~user) (empty? ~user))
     (do ~auth)
     (do ~no-auth)))

