(ns examples.sortable.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [sug.core :as sug :include-macros true]
            [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [goog.style :as gstyle])
  (:import [goog.ui IdGenerator]
           [goog.events EventType]))

(enable-console-print!)

;; =============================================================================
;; Utilities

(defn px [n]
  (if (number? n) (str n "px") n))

(defn location [e]
  [ (.-clientX e) (int (.-clientY e))])

(defn to? [owner next-props next-state k]
  (or (and (not (om/get-render-state owner k))
           (k next-state))
      (and (not (k (om/get-props owner)))
           (k next-props))))

(defn from? [owner next-props next-state k]
  (or (and (om/get-render-state owner k)
           (not (k next-state)))
      (and (k (om/get-props owner))
           (not (k next-props)))))

;; =============================================================================
;; Generic Draggable

(defn drag-start [e data owner]
  (let [loc (location e)]
    (om/set-state! owner :dragging true)
    (om/set-state! owner :start-location loc)
    (om/set-state! owner :last-location loc)
    (sug/fire! owner (or (om/get-state owner :drag-start) :drag-start)
               (conj {:location loc
                      :start-location (om/get-state owner :start-location)
                      :diff-location (map - loc (om/get-state owner :last-location))} (om/get-state owner :message)) )))

(defn drag-stop [e data owner]
  (let [loc (location e)]
    (sug/fire! owner (or (om/get-state owner :drag-stop) :drag-stop)
               (conj {:location loc
                      :start-location (om/get-state owner :start-location)
                      :diff-location (map - loc (om/get-state owner :last-location))} (om/get-state owner :message)))
    (om/set-state! owner :last-location loc)
    (om/set-state! owner :dragging false)))

(defn drag [e data owner]
  (let [loc (location e)]
    (sug/fire! owner (or (om/get-state owner :drag) :drag)
               (conj { :location loc
                       :start-location (om/get-state owner :start-location)
                       :diff-location (map - loc (om/get-state owner :last-location))} (om/get-state owner :message)) )
    (om/set-state! owner :last-location loc)))

(sug/defcomp draggable
  [data owner opts]

  {:init-state
   (fn [_] {:message (:message opts)})

   :will-update
    (fn [_ next-props next-state]
      ;; begin dragging, need to track events on window
      (when (or (to? owner next-props next-state :dragging))
        (let [mouse-up   #(drag-stop % @next-props owner)
              mouse-move #(drag % @next-props owner)]
          (om/set-state! owner :window-listeners
            [mouse-up mouse-move])
          (doto js/window
            (events/listen EventType.MOUSEUP mouse-up)
            (events/listen EventType.MOUSEMOVE mouse-move))))
      ;; end dragging, cleanup window event listeners
      (when (from? owner next-props next-state :dragging)
        (let [[mouse-up mouse-move]
              (om/get-state owner :window-listeners)]
          (doto js/window
            (events/unlisten EventType.MOUSEUP mouse-up)
            (events/unlisten EventType.MOUSEMOVE mouse-move)))))

    :will-unmount
    (fn [_]
      (let [[mouse-up mouse-move]
              (om/get-state owner :window-listeners)]
          (doto js/window
            (events/unlisten EventType.MOUSEUP mouse-up)
            (events/unlisten EventType.MOUSEMOVE mouse-move))))

    :render-state
    (fn [_ state]
       (let [content (:content opts)
             class-name (or (:className opts) "")
             style (or (:style opts)  #js {})]

         (dom/div #js {:className class-name :style style
                       :onMouseDown #(drag-start % data owner)} content)))})



;;============= APP


(sug/defcomp item [data owner]
  {:render-state
  (fn [_ state]
    (when-not (:active state) (om/transact! data :top #(* (:idx state) 35)))
    (dom/div #js {:className (str "item " (:active state)) :style #js {:top (px (:top data)) }}
        (sug/make draggable data {:opts {:className "dragbox"
                                         :message {:uid (:uid state)}
                                         :content (str  (:idx state) "  " (:uid state))}})))

   :on {:drag-start (fn [e] (om/set-state! owner :active "active "))
        :drag (fn [e]
                (let [[dx dy] (:diff-location e)]
                    (om/transact! data :top #(+ % dy))))
        :drag-stop (fn [e]
                     (om/set-state! owner :active nil)
                     (om/transact! data :top #(* (om/get-state owner :idx) 35)))}})

(sug/defcomp sortof [data owner]
  {:render-state
   (fn [_ state]
     (let [last-sort (or (:sorted state) {})
           sorted (into {} (map-indexed
                            (fn [idx v] {(:uid v) idx})
                            (sort-by :top (vals (:items data) ))))]

       ;it's running in a loop with this, but that's the reason the code is short.
      (om/set-state! owner :sorted sorted)

     (apply dom/div nil
        (dom/h2 nil "Sortable example")
        (map (fn [k]
                (sug/make item (k (:items data))
                          {:init-state{:uid k :idx (k sorted)}
                           :state (if (= (k sorted) (k last-sort)) {:idx (k sorted)} {} ) }))
             (keys (:items data))) )))})



(def APP
  (atom {:items (into {} (map-indexed
                       (fn [idx v] {(keyword v) {:uid (keyword v) :top (* idx 35) } })
                       (seq "abcdefghijklmnopqrxyuz") )) }))



(om/root APP sortof (.-body js/document))