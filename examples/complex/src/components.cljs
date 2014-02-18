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


(sug/defcomp render-count [data owner]
  {:did-mount
   (fn [_ _] (sug/private! owner :count 0))
   :render-state
   (fn [_ state]
     (let [renders (sug/private owner :count)]
     (sug/private! owner :count (inc renders))
     (dom/div #js {:className "render-counter"}
        (dom/p nil (str (inc renders) )))))})


(defn bool-box [data owner opts]
  (reify
    om/IInitState
    (init-state [_] {:value (:value data)})
    om/IRender
    (render [_]
      (let [text (or (:text data) "")
            value (or (:value data) false)]

       (dom/label #js {:className "bool"}
           (dom/p nil text)
           (dom/input #js {:type "checkbox"
                           :checked (:value data)
                           :onChange (fn [e]
                                       (let [toggle (not (:value data))]
                                         (om/transact! data [:value] not)))}))))))


(sug/defcomp modal-box [data owner opts]
  {:will-update
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
                                        (when (= (:active data) part) "active ")
                                        (cond (= 0 idx) "top_left bottom_left "
                                              (= amount (inc idx)) "top_right bottom_right "
                                              :else "middle "))]
                     (dom/span #js {:className class-str
                                    :style #js {:width (str (/ 100 amount ) "%")}
                                    :onClick #(click-mode-option data owner part)} part))) (:options data))))))})

(defn collapse-node [start]
  (loop [data start result []]
    (let [d (if (sequential? data) data [data])
          sorted  (map (fn [v]
                         {:id (:uid v) :babies (:children v)}) d)
          uids (flatten (filter #(not (nil? %)) (map :id sorted)))
          babies (vec (flatten (filter #(not (nil? %)) (map :babies sorted))))]

      (if-not (pos? (count babies))
        (concat result uids)
        (recur babies (concat result uids))))))

 (sug/defcomp dom-node [data owner opts]
  {:render-state
   (fn [_ state]
            (let [tag (:tag data)
                  uid (:uid data)
                  targeted (= (:mouse-target state) uid)
                  selected ((:selection state) uid)

                  selected-class (str (when selected "selected ")
                                       (when targeted "targeted ") "")
                  node-class (str
                              "outliner_node "
                              (when (expanded? data) "open "))
                  exp-class (cond (children? data) (cond (expanded? data) "exp-box expanded" :else "exp-box collapsed") :else "exp-box")]
            (apply dom/div #js {:className node-class  :onClick (fn [e] (sug/fire! owner :select-node {:uid @uid}) false)}
                     (dom/span #js {:className (str "outliner_background " selected-class)})
                     (dom/span nil
                       (dom/div #js {:className exp-class :onClick (fn [e] (when (:expanded @data)
                                                                             (sug/fire! owner :collapsing-nodes
                                                                                        {:target @data :nodes (collapse-node (:children @data))}))
                                                                           (toggle-expansion e data owner opts) false)}))
                     (dom/span #js {:className (str "tag-name " tag)} tag)
                     (when (:id data) (dom/span #js {:className "id-name"} (:id data)))



                     (when (and (children? data) (expanded? data))
                       (sug/make-all dom-node (:children data)  {:state {:selection (:selection state)}})
                                                                 ; (conj {} (when (= (first (:mouse-target state)) (:uid data))
                                                                  ;   {:state {:mouse-target (rest (:mouse-target state))}}))
                        ))))})


(defn unbind-drag [owner]
  (let [[mouse-up mouse-move] (sug/private owner :event-handlers)]
    (.unbind (js/$ js/window) "mouseup" mouse-up)
    (.unbind (js/$ js/window) "mousemove" mouse-move)))

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
    (sug/private! owner :last-location loc)
    ))

(defn drag-start [e data owner]
  (let [loc (location e)
        mouse-up   #(drag-stop % @data owner)
        mouse-move #(drag % @data owner)]
    (sug/private! owner :event-handlers [mouse-up mouse-move])
    (.bind (js/$ js/window) "mouseup" mouse-up)
    (.bind (js/$ js/window) "mousemove" mouse-move)

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

