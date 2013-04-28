(ns clojure-course-task05.model
  (:require [korma [db :refer :all] [core :refer :all]]
            [feedparser-clj.core :as parser]
            [cemerick.friend [credentials :as creds]])
  (:import java.util.concurrent.Executors
           java.util.concurrent.TimeUnit))


(defdb db (mysql {:user "root" :subname "//localhost:3306/boo?useUnicode=true&characterEncoding=UTF-8"}))

;; (def env (into {} (System/getenv)))
;; (def dbhost (get env "OPENSHIFT_MYSQL_DB_HOST"))
;; (def dbport (get env "OPENSHIFT_MYSQL_DB_PORT"))
;; (def default-conn {:classname "com.mysql.jdbc.Driver"
;;                              :subprotocol "mysql"
;;                              :user "adminhXiXCUn"
;;                              :password "kVuhlpeM6qJH"
;;                              :subname (str "//" dbhost ":" dbport "/readertask05?useUnicode=true&characterEncoding=utf8")
;;                              :delimiters "`"})
;; (defdb db default-conn)


(defentity feed)

(defentity article
  (belongs-to feed))

(defentity user)

(defentity user-feed
  (table "user_feed")
  (belongs-to user)
  (belongs-to feed))

(defentity user-article-status
  (table "user_article_status")
  (belongs-to user)
  (belongs-to article))



;;; user registration

(defn find-user-by-username [username]
  (first (select user (where (= :username username)))))

(defn create-user-if-not-exists [{:keys [username password]}]
  (when-not (find-user-by-username username)
    (insert user (values {:username username :password (creds/hash-bcrypt password)}))))

;;; feeds

(defn parsed-feed-to-model [parsed-feed from-url]
  (-> (select-keys parsed-feed [:title :link])
      (assoc :parse_url from-url)))

(defn fetch-feed [url]
  (parsed-feed-to-model (parser/parse-feed url) url))

(defn find-feed-from-url [from-url]
  (first (select feed
                 (where {:parse_url from-url}))))

(defn add-feed-if-not-exists! [url]
  "Returns pair - feed and flag indicating it was created now"
  (if-let [f (find-feed-from-url url)]
    [f false]
    [(first (vals (insert feed (values [(fetch-feed url)])))) true]))

(defn read-feed-by-id [feed-id]
  (first (select feed (where {:id feed-id}))))

;;; articles

(defn read-article-by-id [article-id]
  (first (select article (where {:id article-id}))))

(defn article-exists? [a]
  (not (empty? (select article
                       (where {:feed_id (:feed_id a)
                               :link (:link a)})))))

(defn article-insert-if-not-exists! [a]
  (when-not (article-exists? a)
    (insert article (values [a]))))


(defn- feed-entry-to-article [entry]
  (-> entry
      (select-keys [:link :title])
      (assoc :description_type (get-in entry [:description :type]))
      (assoc :description_value (get-in entry [:description :value]))
      (assoc :published_date (:published-date entry))))

(defn fetch-feed-articles [f]
  (->> (:entries (parser/parse-feed (:parse_url f)))
       (map feed-entry-to-article)
       (map #(assoc % :feed_id (:id f)))))

(defn save-new-articles! [articles]
  (doseq [a articles]
    (article-insert-if-not-exists! a)))

(defn save-new-feed-articles! [f]
  (save-new-articles! (fetch-feed-articles f)))


;;; utils TODO: move to utils.clj

(defn insert-or-update [entity search-by-fields value]
  (let [search-by (select-keys value search-by-fields)] 
      (if (empty? (select entity (where search-by)))
        (insert entity (values [value]))
        (update entity
                (set-fields (select-keys value (remove (set search-by-fields) (keys value))))
                (where search-by)))))


;;; user feeds association

(defn user-feed-exists? [u f]
  (not (empty? (select user-feed (where {:user_id (:id u)
                                         :feed_id (:id f)})))))

(defn add-user-feed-if-not-exists! [u f]
  (when-not (user-feed-exists? u f)
    (insert user-feed (values [{:user_id (:id u)
                                :feed_id (:id f)}]))))


;;; user articles association

(defn- adjust-articles-status-value-when-empty [articles]
  (map #(assoc % :status (or (:status %) "unread")) articles))

(def insert-or-update-user-article-status
  (partial insert-or-update user-article-status [:user_id :article_id]))

(defn mark-user-article-read-status [u a status]
  (insert-or-update-user-article-status {:user_id (:id u)
                                         :article_id (:id a)
                                         :status status}))

(defn read-article-with-user-status-by-id [u article-id]
  (let [a (read-article-by-id article-id)
        status (:status (first (select user-article-status
                                       (where (and (= :user_id (:id u))
                                                   (= :article_id article-id))))))]
    (first (adjust-articles-status-value-when-empty [(assoc a :status status)]))))

;;; higher level user actions

(defn list-user-feeds [u]
  (select feed (join [user-feed :uf] (= :feed.id :uf.feed_id)) (where (= :uf.user_id (:id u)))))

(defn- make-user-articles-customized-query [u max customize-fn]
  (customize-fn (-> (select* article)
                    (fields :* :uas.status)
                    (join :left [user-article-status :uas] (and (= :article.id :uas.article_id)
                                                                (= :uas.user_id (:id u))))
                    (order :article.published_date :desc)
                    (limit max))))

(defn- list-user-articles-with-customized-query [u max customize-fn]
  (-> (make-user-articles-customized-query u max customize-fn)
      select
      adjust-articles-status-value-when-empty))

(defn list-new-user-articles-by-feed
  ([u f] (list-new-user-articles-by-feed u f 1000))
  ([u f max]
     (list-user-articles-with-customized-query u max
       #(where % (and (or (= :uas.status nil)
                          (= :uas.status "unread"))
                      (= :article.feed_id (:id f)))))))


(defn fetch-feeds-updates
  ([] (fetch-feeds-updates (select feed)))
  ([feeds]
     (doseq [a (mapcat fetch-feed-articles feeds)]
       (article-insert-if-not-exists! a))))

(defn schedule-feeds-updates [every-secs]
  (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1)
                        #(try
                           (println "Starting feeds update..")
                           (fetch-feeds-updates)
                           (println "Updated feeds successfully")
                           (catch Exception e
                             ;; TODO: logging, not printing
                             (println "Error updating feeds:" (.getMessage e) (.getClass e))))
                        0 every-secs TimeUnit/SECONDS))


(defn subscribe-user-to-feed [u from-url]
  (let [[f created?] (add-feed-if-not-exists! from-url)]
    (add-user-feed-if-not-exists! u f)
    (if created? (fetch-feeds-updates [f]))
    f))


