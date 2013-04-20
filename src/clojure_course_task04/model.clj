(ns clojure-course-task04.model
  (:require [korma [db :refer :all] [core :refer :all]]
            [feedparser-clj.core :as parser]))


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

(defn list-new-user-articles
  ([u] (list-new-user-articles u 1000))
  ([u max]
     (->> (select article
                  (fields :* :uas.status)
                  (join :left [user-article-status :uas] (= :article.id :uas.article_id))
                  (where (and (in :article.feed_id (subselect user-feed
                                                              (fields :feed_id)
                                                              (where (= :user_id (:id u)))))
                              (or (= :uas.status nil)
                                  (= :uas.status "unread"))))
                  (order :article.published_date :desc)
                  (limit max))
          (map #(assoc % :status (or (:status %) "unread"))))))


(defn fetch-feeds-updates [feeds]
  (doseq [a (mapcat fetch-feed-articles feeds)]
    (article-insert-if-not-exists! a)))

(def ^:private feed-update-agent (agent nil)) ; using agent just to queue fetch jobs

(defn trigger-feeds-updates
  ([] (trigger-feeds-updates (select feed)))
  ([feeds] (send-off feed-update-agent #(fetch-feeds-updates %2) feeds)))

(defn subscribe-user-to-feed [u from-url]
  (let [id (add-feed-if-not-exists! from-url)
        f (first (select feed (where {:id id})))]
    (add-user-feed-if-not-exists! u f)
    (trigger-feeds-updates [f])))


