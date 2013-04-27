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


(def ^:dynamic *user* {:id 1})

(defroutes app-routes
  (GET "/" [] (resp/redirect "/feeds"))
  (GET "/feeds" [] (view/show-feeds-page *user*))
  (GET "/user-feeds-data" [] (view/user-feeds-data *user*))
  (POST "/subscribe-to-feed" [url] (view/subscribe-to-feed *user* url))
  (GET "/user-feed-articles" [feed_id] (view/user-feed-articles *user* (Integer/parseInt feed_id))))


(def app (-> [(handler/site #'app-routes)]
             noir/app-handler
             noir/war-handler))

(comment
  (defonce server (jetty/run-jetty #'app {:port 3000 :join? false})))
