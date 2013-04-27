(ns clojure-course-task05.model
  (:require [korma [db :refer :all] [core :refer :all]]
            [feedparser-clj.core :as parser]
            [cemerick.friend [credentials :as creds]]))


(defdb db (mysql {:user "root" :subname "//localhost:3306/boo?useUnicode=true&characterEncoding=UTF-8"}))


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
  (if-let [f (find-feed-from-url url)]
    (:id f)
    (first (vals (insert feed (values [(fetch-feed url)]))))))

(defn read-feed-by-id [feed-id]
  (first (select feed (where {:id feed-id}))))

;;; articles

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

(def insert-or-update-user-article-status
  (partial insert-or-update user-article-status [:user_id :article_id]))

(defn mark-user-article-read-status [u a status]
  (insert-or-update-user-article-status {:user_id (:id u)
                                         :article_id (:id a)
                                         :status status}))

(defn mark-user-article-read [u a]
  (mark-user-article-read-status u a "read"))

(defn mark-user-article-unread [u a]
  (mark-user-article-read-status u a "unread"))


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

(defn- adjust-articles-status-value-when-empty [articles]
  (map #(assoc % :status (or (:status %) "unread")) articles))

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


(defn fetch-feeds-updates [feeds]
  (doseq [a (mapcat fetch-feed-articles feeds)]
    (article-insert-if-not-exists! a)))

(def ^:private feed-update-agent (agent nil)) ; using agent just to queue fetch jobs

(defn trigger-feeds-updates
  ([] (trigger-feeds-updates (select feed)))
  ([feeds] (send-off feed-update-agent #(fetch-feeds-updates %2) feeds)))

;;; TODO: add 'cron' job to periodically update feeds articles

(defn subscribe-user-to-feed [u from-url]
  (let [id (add-feed-if-not-exists! from-url)
        f (first (select feed (where {:id id})))]
    (add-user-feed-if-not-exists! u f)
    (fetch-feeds-updates [f])           ; TODO: should this be only for new feeds - not yet in system?
    f))


