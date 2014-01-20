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
  (let [up (chan)
        down (chan)]
  {:chan-up up
   :chan-down down
   :pub-up (pub up map?)
   :pub-down (pub down map?)}))


(defn -register [exant new]
  (let [unmade (filter #(not (% exant)) (keys new))
        clones (zipmap (keys exant) (repeatedly #(-origin)))
        virgin (zipmap unmade (repeatedly #(-origin)))]
    (dorun (map (fn [ex cl]
                  (sub (:pub-up cl) true (:chan-up ex))
                  (sub (:pub-down ex) true (:chan-down cl))
                  ) (vals exant) (vals clones) ))
   (merge clones virgin)))

(defn p-sub->c [p]
  (let [c (chan)]
        (sub p true c) c))

(defn -fire! [dir owner k e]
  (when-let [chans (om/get-state owner :_events)]
    (let [kcol (if (sequential? k) k [k])]
      (dorun (for [sk kcol]
        (when-let [target (sk chans)]
          (when (#{:down :both} dir) (put! (:chan-down target) e))
          (when (#{:up :both} dir) (put! (:chan-up target) e)) ))))))


(defn fire! [o k e]
  (-fire! :up o k e))

(defn fire-up! [o k e]
  (-fire! :up o k e))

(defn fire-down! [o k e]
  (-fire! :down o k e))

(defn fire-both! [o k e]
  (-fire! :both o k e))

(defn -event-setup [app owner]
  (let [state (om/get-state owner)]
    (when-let [published (:_events state)]
      (when-let [handlers (:_event_handlers state)]
        (let [hk (keys handlers)
              pkv (select-keys published hk)]
          (dorun (for [k (keys pkv)
                 :let [pub-down (:pub-down (k pkv))
                       pub-up (:pub-up (k pkv))
                       f (k handlers)
                       c-d (p-sub->c pub-down)
                       c-u (p-sub->c pub-up)]]
               (do
                 (go (while true (f (<! c-u) app owner )))
                 (go (while true (f (<! c-d) app owner )))
                  ))))))))






