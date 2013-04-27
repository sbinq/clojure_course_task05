(ns clojure-course-task05.main
  (:require [clojure.browser.repl :as repl]
            [enfocus.core :as ef]
            [goog.events :as gevents]
            [helpers.util :as util]
            goog.ui.Prompt
            goog.i18n.DateTimeFormat)
  (:require-macros [enfocus.macros :as em])
  (:use [jayq.core :only [$ css inner]]))


(repl/connect "http://localhost:9000/repl")

;;; TODO: move to utils
(defn log [& args]
  (.call (.-log js/console) js/console (apply str args)))

(defn format-date [jsdate]
  (.format (goog.i18n.DateTimeFormat. "yyyy/MM/dd, HH:mm") jsdate))

;;; Feeds list

(declare feed-list-item-ui-id)
(declare try-update-feed-articles)

(em/defaction mark-feed-list-items-non-active []
  [".feed-list li"] (em/remove-class "active"))

(defn mark-feed-list-item-active [f]
  (mark-feed-list-items-non-active)
  (em/at js/document
         [(str "#" (feed-list-item-ui-id f))] (em/add-class "active")))

(defn on-feed-menu-item-click [f e]
  (try-update-feed-articles f)
  (mark-feed-list-item-active f))

(defn feed-list-item-ui-id [f]
  (str "feed-" (:id f)))

(em/defsnippet feed-list-items "html/fragments.html" ["#feed-list-items-template"] [feeds & [selected-feed]]
  [".feed-menu-item"] (em/clone-for [f feeds]
                                    (em/do-> (em/set-attr :id (feed-list-item-ui-id f))
                                             (if (= (:id f) (:id selected-feed))
                                               (em/add-class "active")
                                               identity)
                                             #(em/at % ["a"] (em/content (:title f)))
                                             (em/listen :click (fn [e] (on-feed-menu-item-click f e)))))
  [".no-items-message"] (if (not (empty? feeds))
                          (em/remove-node)
                          identity)
  ["ul"] (em/unwrap))

(em/defaction update-feed-list-items [feeds & [selected-feed]]
  [".feed-menu-item, .no-items-message"] (em/remove-node)
  [".subscriptions-list-header"] (em/after (feed-list-items feeds selected-feed)))

(defn try-update-feed-list-items []
  (util/get-data "/user-feeds-data" update-feed-list-items))


;;; Feed articles

(em/defsnippet articles-list "html/fragments.html" ".feed-articles" [articles]
  [".feed-article-row"] (em/clone-for [a articles]
                                      [".article-title"] (em/html-content (or (:title a) "(title unknown)"))
                                      [".article-description"] (em/html-content (:description_value a))
                                      [".article-link"] (em/set-attr :href (:link a))
                                      [".article-published-date"] (em/content (format-date (:published_date a)))))

(em/defaction update-feed-articles [f {:keys [articles] :as response}]
  [".feed-header .feed-title"] (em/content (:title f))
  [".feed-header .feed-link"] (em/set-attr :href (:link f))
  [".feed-articles"] (em/substitute (articles-list articles)))

(defn try-update-feed-articles [f]
  (util/get-data (str "/user-feed-articles?feed_id=" (:id f)) #(update-feed-articles f %)))

;;; Feeds subscription

(defn prompt-feed-url [callback]
  (let [update-buttons-css (fn [prompt]
                             (em/at (.getButtonElement prompt)
                                    ["button"] (em/add-class "btn" "btn-small")))]
    (doto (goog.ui.Prompt. "Please Provide Feed URL" "" callback)
      (.setContent "")
      update-buttons-css
      (.setVisible true))))


(defn subscribed-to-new-feed [feeds new-feed]
  (update-feed-list-items feeds new-feed))

(defn maybe-subscribed-to-new-feed [{:keys [error feeds new-feed] :as response}]
  (if (empty? error)
    (subscribed-to-new-feed feeds new-feed)
    (js/alert error)))

(defn try-subscribe-to-feed [feed-url]
  (util/post-data "/subscribe-to-feed" maybe-subscribed-to-new-feed {:url feed-url}))


(defn on-subscribe-click []
  (prompt-feed-url #(let [trimmed (and % (clojure.string/trim %))]
                      (when-not (empty? trimmed) (try-subscribe-to-feed trimmed)))))



;;; Unread articles summary

(em/defaction show-user-articles-summary []
  )



;;; Page initializion

(em/defaction setup-listeners []
  [".subscribe-to-feed-button"] (em/listen :click on-subscribe-click))

(defn setup []
  (setup-listeners)
  (em/wait-for-load (try-update-feed-list-items)))

(set! (.-onload js/window) setup)
