(ns clojure-course-task05.main
  (:require [clojure.browser.repl :as repl]
            [enfocus.core :as ef]
            [goog.events :as gevents]
            [helpers.util :as util]
            goog.ui.Prompt)
  (:require-macros [enfocus.macros :as em])
  (:use [jayq.core :only [$ css inner]]))


(repl/connect "http://localhost:9000/repl")


(defn on-feed-menu-item-click [f e]
  (.log js/console "on-feed-menu-item-click" f e))

(em/defsnippet feed-list-items "html/fragments.html" ["#feed-list-items-template"] [feeds & [selected-feed]]
  [".feed-menu-item"] (em/clone-for [f feeds]
                                    (em/do-> (em/set-attr :id (str "feed-" (:id f)))
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
  (.log js/console "inside try-update-feed-list-items")
  (util/get-data "/user-feeds-data" update-feed-list-items))


(defn prompt-feed-url [callback]
  (let [update-buttons-css (fn [prompt]
                             (em/at (.getButtonElement prompt)
                                    ["button"] (em/add-class "btn" "btn-small")))]
    (doto (goog.ui.Prompt. "Please Provide Feed URL" "" callback)
      (.setContent "")
      update-buttons-css
      (.setVisible true))))

;;; TODO: move to utils
(defn log [& args]
  (.call (.-log js/console) js/console (apply str args)))

(defn subscribed-to-new-feed [feeds new-feed]
  (log "all feeds are" feeds)
  (update-feed-list-items feeds new-feed))

(defn maybe-subscribed-to-new-feed [{:keys [error feeds new-feed] :as response}]
  (log "response is " response)
  (log "feeds are " feeds)
  (log "new-feed is " new-feed)
  (if (empty? error)
    (subscribed-to-new-feed feeds new-feed)
    (js/alert error)))

(defn try-subscribe-to-feed [feed-url]
  (util/post-data "/subscribe-to-feed" maybe-subscribed-to-new-feed {:url feed-url}))


(defn on-subscribe-click []
  (prompt-feed-url #(let [trimmed (and % (clojure.string/trim %))]
                      (when-not (empty? trimmed) (try-subscribe-to-feed trimmed)))))


(em/defaction show-user-articles-summary []
  )


(em/defaction setup-listeners []
  [".subscribe-to-feed-button"] (em/listen :click on-subscribe-click))

(defn setup []
  (setup-listeners)
  (em/wait-for-load (try-update-feed-list-items)))

(set! (.-onload js/window) setup)
