(ns examples.sortable.core
  (:require [sug.core :as sug :include-macros true]
            [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events])
  (:import [goog.events EventType]))

(enable-console-print!)

;; =============================================================================
;; Utilities

(defn px [n]
  (if (number? n) (str n "px") n))

(defn location [e]
  [ (.-clientX e) (int (.-clientY e))])

;; =============================================================================
;; Generic Draggable



(defn unbind-drag [owner]
  (let [[mouse-up mouse-move] (sug/private owner :event-handlers)]
    (doto js/window
            (events/unlisten EventType.MOUSEUP mouse-up)
            (events/unlisten EventType.MOUSEMOVE mouse-move))))

(defn drag-stop [e data owner]
  (let [loc (location e)]
    (sug/fire! owner (or (om/get-state owner :drag-stop) :drag-stop)
               (conj {:location loc
                      :start-location (sug/private owner :start-location)
                      :diff-location (map - loc (sug/private owner :last-location))} (om/get-state owner :message)))
    (unbind-drag owner)))

(defn drag [e data owner]
  (let [loc (location e)]
    (sug/fire! owner (or (om/get-state owner :drag) :drag)
               (conj { :location loc
                       :start-location (sug/private owner :start-location)
                       :diff-location (map - loc (sug/private owner :last-location))} (om/get-state owner :message)) )
    (sug/private! owner :last-location loc)))

(defn drag-start [e data owner]
  (let [loc (location e)
        mouse-up   #(drag-stop % @data owner)
        mouse-move #(drag % @data owner)]
    (sug/private! owner :event-handlers [mouse-up mouse-move])
    (doto js/window
      (events/listen EventType.MOUSEUP mouse-up)
      (events/listen EventType.MOUSEMOVE mouse-move))
    (sug/private! owner :start-location loc)
    (sug/private! owner :last-location loc)
    (sug/fire! owner (or (om/get-state owner :drag-start) :drag-start)
               (conj {:location loc
                      :start-location loc
                      :diff-location [0 0]} (om/get-state owner :message)) )))

(sug/defcomp draggable
  [data owner opts]

  {:will-unmount
    (fn [_]
      (unbind-drag owner))

    :render-state
    (fn [_ state]
       (let [content (:content opts)
             class-name (or (:className opts) "")
             style (or (:style opts)  #js {})]

         (dom/div #js {:className class-name :style style
                       :onMouseDown #(drag-start % data owner)} content)))})



;;============= APP

(sug/defcomp render-count [data owner]
  {:did-mount
   (fn [_] (sug/private! owner :count 0))
   :render-state
   (fn [_ state]
     (let [renders (sug/private owner :count)]
     (sug/private! owner :count (inc renders))
     (dom/div #js {:className "render-counter"}
        (dom/p nil (str "renders: " (inc renders) )))))})

(sug/defcomp item [data owner]
  {:render-state
  (fn [_ state]
    (let [top (or (sug/private owner :top) (:top data))]
    (when-not (:active state) (om/transact! data :top #(* (:idx state) 35)))
    (dom/div #js {:ref "item" :className (str "item " (:active state)) :style #js {:top (px top) }}
        (sug/make render-count data {})
        (sug/make draggable data {:init-state {:message {:uid (:uid state)}}
                                  :opts {:className "dragbox"
                                         :content (str  (:idx state) "  " (:uid state))}}))))

   :on {:drag-start (fn [e] (om/set-state! owner :active "active ")
                      (sug/private! owner :top  (* (om/get-state owner :idx) 35)))
        :drag (fn [e]
                (let [[dx dy] (:diff-location e)
                      node (om/get-node owner "item")
                      top (sug/private owner :top)
                      new-top (+ top dy)
                      idx (int (/ (+ new-top 1) 35))]
                    (aset (.-style node) "top" (px new-top))
                    (sug/private! owner :top new-top)
                    (when (not= (sug/private owner :idx) idx)
                      (do  (sug/private! owner :idx idx)
                          (om/transact! data :top #(identity new-top))))))
        :drag-stop (fn [e]
                     (om/set-state! owner :active nil)
                     (sug/private! owner :top false)
                     (sug/private! owner :idx nil)
                     (om/transact! data :top #(* (om/get-state owner :idx) 35)))}})


(sug/defcomp sortof [data owner]
  {:render-state
   (fn [_ state]
     (let [last-sort (or (sug/private owner :sorted) {})
           sorted (into {} (map-indexed
                            (fn [idx v] {(:uid v) idx})
                            (sort-by :top (vals (:items data) ))))]

     (sug/private! owner :sorted sorted)

     (apply dom/div nil
            (dom/div #js {:className "title"}
                     (dom/h2 nil "Sortable example")
                     (sug/make render-count data {}))
        (map (fn [k]
                (sug/make item (k (:items data))
                          {:react-key k
                           :init-state {:uid k :idx (k sorted)}
                           :state (if (not= (k sorted) (k last-sort)) {:idx (k sorted)} {})}))
             (keys (:items data))) )))})


(def APP
  (atom {:items (into {} (map-indexed
                       (fn [idx v] {(keyword v) {:uid (keyword v) :top (* idx 35) } })
                       (seq "abcdefghijklmnopqrxyuz") )) }))


(om/root sortof APP {:target (.-body js/document)})

(sug/defcomp b [data owner]
  {:should-update
   (fn [_ next-props next-state]
     (prn "B" next-state (om/get-render-state owner))
     true)
   :render-state
   (fn [_ state]
     (dom/div nil
              (dom/button #js {:onClick #(om/set-state! owner :g (rand-int 100))} "B")
              ))})

(sug/defcomp a [data owner]
  {:should-update
   (fn [_ next-props next-state]
     (prn "A" next-state (om/get-render-state owner))
     true)
   :render-state
   (fn [_ state]
     (dom/div nil
              (dom/span nil "A")
              (sug/make b data {:init-state {:g -1}})))})

(om/root a {} {:target (.-body js/document)})
