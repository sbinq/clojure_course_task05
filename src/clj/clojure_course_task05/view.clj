(ns clojure-course-task05.view
  (:require [me.raynes.laser :as l]
            [clojure.java.io :refer [file]]
            [clojure-course-task05.model :as model]))


(defn show-feeds-page [u]               ; TODO: at least username in template
  (clojure.java.io/resource "public/html/main.html"))

(defn user-feeds-data [u]
  (pr-str (model/list-user-feeds u)))
