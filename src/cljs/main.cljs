(ns sug.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [sug.core :refer [defcomp make]]
                   )
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [cljs.core.async :as async :refer [>! <! <!! put! chan pipe pub sub close!]]))

(defn state
  ([owner]
    (om/get-state owner))
  ([owner & more]
    (om/get-state owner more)))

(defn state!
  [owner korks v]
    (om/set-state! owner korks v))

(defn -origin []
  (let [c (chan)]
  {:chan c
   :pub (pub c map?)}))


(defn -register [exant new]
  (let [unmade (filter #(not (% exant)) (keys new))
        clones (zipmap (keys exant) (repeatedly #(-origin)))
        virgin (zipmap unmade (repeatedly #(-origin)))]
    (dorun (map (fn [ex cl]
                  ; Useful top down flow
                  ;(sub (:pub ex) true (:chan cl))
                  (sub (:pub cl) true (:chan ex))
                  ) (vals exant) (vals clones) ))
   (merge clones virgin)))

(defn p-sub->c [p]
  (let [c (chan)]
        (sub p true c) c))

(defn fire! [owner k e]
  (when-let [chans (om/get-state owner :_events)]
    (let [kcol (if (sequential? k) k [k])]
      (dorun (for [sk kcol]
        (when-let [target (sk chans)]
          (put! (:chan target) e) ))))))

(defn -event-setup [app owner]
  (let [state (om/get-state owner)]
    (when-let [published (:_events state)]
      (when-let [handlers (:_event_handlers state)]
        (let [hk (keys handlers)
              pkv (select-keys published hk)]
          (dorun (for [k (keys pkv)
                 :let [pub (:pub (k pkv))
                       f (k handlers)
                       c (p-sub->c pub)]]
                 (go (while true (f (<! c) app owner ))))))))))






