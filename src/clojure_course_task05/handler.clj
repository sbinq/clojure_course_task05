(ns clojure-course-task05.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [noir.util.middleware :as noir]
            [noir.session :as session]
            [ring.adapter.jetty :as jetty]
            [clojure-course-task05.view :as view]))

;;; TODO: implement unread articles summary page - all feeds
;;; TODO: implement feed-specific page with "Unsubscribe" and "Mark All Read" actions; optionally "N New Items" / "All Items" switch (+ pagination?)
;;; TODO: implement new users registration; optionally - with many auth backends; optionally - admin user with misc administrative UI;

(defroutes app-routes
  (GET "/" [] (resp/redirect "/feeds"))
  (GET "/feeds" [] (view/show-feeds)))

(def app (-> [(handler/site #'app-routes)]
             noir/app-handler
             noir/war-handler))

(comment
  (defonce server (jetty/run-jetty #'app {:port 3000 :join? false})))
