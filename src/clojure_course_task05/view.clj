(ns clojure-course-task05.view
  (:require [me.raynes.laser :as l]
            [clojure.java.io :refer [file]]))


(defn show-feeds []
  (clojure.java.io/resource "public/html/main.html"))
