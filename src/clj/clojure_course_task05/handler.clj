(ns clojure-course-task05.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [noir.util.middleware :as noir]
            [noir.session :as session]
            [ring.adapter.jetty :as jetty]
            [clojure-course-task05.view :as view]
            [clojure-course-task05.model :as model]
            [cemerick.friend :as friend]
            [cemerick.friend [workflows :as workflows] [credentials :as creds]]))


(defmacro with-user-arg [req form]
  (let [form-with-user-arg (cons (first form)
                                 (cons `(model/find-user-by-username (:current (friend/identity ~req)))
                                       (next form)))]
    `(friend/authenticated
      ~form-with-user-arg)))

(defroutes app-routes
  (GET "/login" []
       (view/show-login-page))
  (friend/logout (ANY "/logout" request
                      (ring.util.response/redirect "/login")))
  
  (GET "/" []
       (resp/redirect "/feeds"))  
  (GET "/feeds" req
       (with-user-arg req
         (view/show-feeds-page)))
  (GET "/user-feeds-data" req
       (with-user-arg req
         (view/user-feeds-data)))
  (GET "/user-feed-articles" [feed_id :as req]
       (with-user-arg req
         (view/user-feed-articles (Integer/parseInt feed_id))))

  (POST "/subscribe-to-feed" [url :as req]
        (with-user-arg req
          (view/subscribe-to-feed url)))
  (POST "/mark-article-read-status" [article_id status :as req]
        (with-user-arg req
          (view/mark-article-read-status (Integer/parseInt article_id) status))))


(def app (-> #'app-routes
             (friend/authenticate {:credential-fn (fn [params]
                                                    (model/create-user-if-not-exists params) ; simplifying registration impl a bit
                                                    (creds/bcrypt-credential-fn model/find-user-by-username params))
                                   :workflows [(workflows/interactive-form)]})
             handler/site
             vector
             noir/app-handler
             noir/war-handler))

(defn init []
  ;; TODO: logging!
  (println "Initializing feeds update scheduler")
  (model/schedule-feeds-updates 3600)
  (println "Finished"))

(comment
  (defonce server (jetty/run-jetty #'app {:port 3000 :join? false})))
