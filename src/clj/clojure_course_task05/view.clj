(ns clojure-course-task05.view
  (:require [me.raynes.laser :as l]
            [clojure.java.io :refer [file]]
            [clojure-course-task05.model :as model]))


(defn show-login-page []
  (clojure.java.io/resource "public/html/signin.html"))

(defn show-feeds-page [u]
  (l/document (l/parse (clojure.java.io/resource "public/html/main.html"))
              (l/class= "logged-in-username") (l/content (:username u))))

(defn user-feeds-data [u]
  (pr-str (model/list-user-feeds u)))

(defn subscribe-to-feed [u url]
  (pr-str
   (try
     (let [new-feed (model/subscribe-user-to-feed u url)
           feeds (model/list-user-feeds u)]
       {:feeds feeds :new-feed new-feed})
     (catch Exception e
       ;; TODO: logging?
       {:error (str "Error: " (.getMessage e) " of " (.getClass e))}))))

(defn user-feed-articles [u feed-id]
  (pr-str {:articles (model/list-new-user-articles-by-feed u (model/read-feed-by-id feed-id))}))
