(ns clojure-course-task05.view
  (:require [me.raynes.laser :as l]
            [clojure.java.io :refer [file]]
            [clojure-course-task05.model :as model]))


(defn show-feeds-page [u]               ; TODO: at least username in template
  (clojure.java.io/resource "public/html/main.html"))

(defn user-feeds-data [u]
  (pr-str (model/list-user-feeds u)))

(defn subscribe-to-feed [u url]
  ;; TODO: continue here; implement parsing feed if it is new in the same thread
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
