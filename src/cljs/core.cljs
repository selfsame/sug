(ns sug.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   ;[sug.core :refer [defcomp]]
                   )
  (:require
      ;[om.core :as om :include-macros true]
      ;[om.dom :as dom :include-macros true :refer []]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]))

(defn state
  ([owner]
    (om/get-state owner))
  ([owner & more]
    (om/get-state owner more)))

(defn state!
  [owner korks v]
    (om/set-state! owner korks v))

(defn fire! [owner k e]
  (when-let [chans (state owner :_events)]
    (let [kcol (if (sequential? k) k [k])]
      (dorun (for [sk kcol]
        (when-let [target (sk chans)]
          (put! (:chan target) e) ))))))

(defn -event-setup [app owner]
  (when-let [chan-map (state owner :_events)]
    (dorun (for [v (vals chan-map)
          :let [c (:chan v)
                f (:f v)]]
       (do (go (while true
          (alt! c ([e c] (f app owner e)) )))  )))))

