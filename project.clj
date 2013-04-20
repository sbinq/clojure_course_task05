(defproject myblog "0.1.0-SNAPSHOT"
  :description "Yet another google reader alternative which is not even planned to be finished"
  :url "https://github.com/sbinq/clojure_course_task04"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [me.raynes/laser "1.1.1"]
                 [mysql/mysql-connector-java "5.1.24"]
                 [korma "0.3.0-RC5"]
                 [lib-noir "0.4.9"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler clojure-course-task05.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
