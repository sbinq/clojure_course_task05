(defproject readertask05 "0.1.0-SNAPSHOT"
  :description "Yet another google reader alternative which is not even planned to be finished"
  :url "https://github.com/sbinq/clojure_course_task05"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [me.raynes/laser "1.1.1"]
                 [mysql/mysql-connector-java "5.1.24"]
                 [korma "0.3.0-RC5"]
                 [lib-noir "0.4.9"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [enfocus "1.0.1"]
                 [jayq "2.3.0"]
                 [com.cemerick/piggieback "0.0.4"]
                 [com.cemerick/friend "0.1.4"]]
  :plugins [[lein-ring "0.8.3"] [lein-cljsbuild "0.3.0"]]
  :ring {:handler clojure-course-task05.handler/app
         :init clojure-course-task05.handler/init }
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}}
  
  :aot []
  :source-paths ["src/clj" "src/cljs"]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :cljsbuild
  {:builds
   [
    {:source-paths ["src/cljs"],
     :id "main",
     :compiler
     {:pretty-print true,
      :output-to "resources/public/assets/js/main.js",
      :warnings true,
      :externs ["externs/jquery-1.9.js"],
      ;; :optimizations :advanced,
      :optimizations :whitespace,
      :print-input-delimiter false}}
    ]
   }
  
  :war {:name "reader.war"})
