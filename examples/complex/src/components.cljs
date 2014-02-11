(ns examples.complex.components
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
   [sug.core :as sug :include-macros true]
      [cljs.reader :as reader]
   [cljs.core.async :as async :refer [>! <! put! chan]]
      [goog.events :as events]
      )
  (:use
    [examples.complex.data :only [UID INITIAL]]
   [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within? get-xywh element-dimensions element-offset]])
   (:import [goog.ui IdGenerator]
           [goog.events EventType]))


(defn children? [data]
  (pos? (count (:children data))))

(defn expanded? [data]
  (:expanded data))

(defn toggle-expansion [e data owner opts]
  (om/transact! data :expanded not))

(defn click-mode-option [data owner option]
  (om/set-state! owner :active option)
  (om/transact! data [:active] #(identity option)))



(defn bool-box [data owner opts]
  (reify
    om/IInitState
    (init-state [_] {:value (:value data)})
    om/IRender
    (render [_]
      (let [state (om/get-state owner)
            text (or (:text data) "")
            value (or (:value data) false)]

       (dom/label #js {:className "bool"}
           (dom/p nil text)
           (dom/input #js {:type "checkbox"
                           :checked (:value state)
                           :onChange (fn [e]
                                       (let [state (om/get-state owner)
                                             toggle (not (:value state))]
                                         (om/set-state! owner :value toggle)
                                         (om/transact! data [:value] #(identity toggle))
                                       ))}))))))


(sug/defcomp modal-box [data owner opts]
  {:init-state
   (fn [_] {:active (:active data)})
   :will-update
   (fn [_ next-props next-state]
                 )
   :render-state
   (fn [_ state]
      (let [amount (count (:options data))
            cname (str "mode-box " (when (true? (:disabled state)) "disabled"))]
      (dom/div #js {:className cname}
           (into-array
            (map-indexed (fn [idx part]
                   (let [class-str (str "option "
                                        (when (= (:active state) part) "active ")
                                        (cond (= 0 idx) "top_left bottom_left "
                                              (= amount (inc idx)) "top_right bottom_right "
                                              :else "middle "))]
                     (dom/span #js {:className class-str
                                    :style #js {:width (str (/ 100 amount ) "%")}
                                    :onClick #(click-mode-option data owner part)} part))) (:options data))))))
   :on {:mode
        (fn [e] (om/set-state! owner :__c (rand)) )}})



 (sug/defcomp dom-node [data owner opts]
  {:render-state
   (fn [_ state]
            (let [tag (:tag data)

                  targeted (= (:mouse-target state) (:uid data))
                  selected ((:selection state) (:uid data))

                  selected-class (str (when selected "selected ")
                                       (when targeted "targeted ") "")
                  node-class (str
                              "outliner_node "
                              (when (expanded? data) "open "))
                  exp-class (cond (children? data) (cond (expanded? data) "exp-box expanded" :else "exp-box collapsed") :else "exp-box")]
            (apply dom/div #js {:className node-class}
                     (dom/span #js {:className (str "outliner_background " selected-class)})
                     (dom/span nil
                       (dom/div #js {:className exp-class :onClick #(toggle-expansion % data owner opts)}))
                     (dom/span #js {:className (str "tag-name " tag)} tag)
                     (when (:id data) (dom/span #js {:className "id-name"} (:id data)))



                     (when (and (children? data) (expanded? data))
                       (sug/make-all dom-node (:children data)  {:state {:selection (:selection state)}})
                                                                 ; (conj {} (when (= (first (:mouse-target state)) (:uid data))
                                                                  ;   {:state {:mouse-target (rest (:mouse-target state))}}))

                                     ) )
                   )) })



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

  {:will-update
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

