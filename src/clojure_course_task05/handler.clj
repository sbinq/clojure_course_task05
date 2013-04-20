(ns clojure-course-task05.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [noir.util.middleware :as noir]
            [noir.session :as session]
            [ring.adapter.jetty :as jetty]
            [clojure-course-task04.view :as view]))

(defroutes app-routes
  (GET "/" [] (resp/redirect "/feeds"))
  (GET "/feeds" [] (view/show-feeds)))

(def app (-> [(handler/site #'app-routes)]
             noir/app-handler
             noir/war-handler))

(comment
  (defonce server (jetty/run-jetty #'app {:port 3000 :join? false})))
