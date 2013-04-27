(ns helpers.components
  (:require [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(em/defsnippet dialog-snipp "/html/fragments.html" [:#modal-dialog]
  [{:keys [width height header body footer]}]
  [:#content] (let [w (if-not (nil? width) (str "width:" width "px;") "")
                    h (if-not (nil? height) (str "height:" height "px;") "")]
                (em/set-attr :style (str w h)))
  [:#header] (em/content header)
  [:#body] (em/content body)
  [:#footer] (em/content footer))


(defn ^:export show-dialog [params]
  (em/at js/document
         [:#dialog-holder] (em/content (dialog-snipp params))))

(defn ^:export hide-dialog []
  (em/at js/document
         [:#dialog-holder] (em/content "")))

(em/defsnippet ok-cancel-buttons "/html/fragments.html" [:#ok-cancel]
  [js-func]
  [:#ok-btn] (em/set-attr :onclick js-func))

(defn show-ok-cancel-dialog [txt js-func]
  (show-dialog {:header "Confirmation dialog",
                :body txt,
                :footer (ok-cancel-buttons js-func),
                :width 315}))
