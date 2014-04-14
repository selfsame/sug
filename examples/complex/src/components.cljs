(ns examples.complex.components
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
   [clojure.walk :as walk]
   [clojure.zip :as zip]
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
   [sug.core :as sug :include-macros true]
      [cljs.reader :as reader]
   [cljs.core.async :as async :refer [>! <! put! chan]]
      [goog.events :as events]
      )
  (:use
    [examples.complex.data :only [UID]]
   [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within? get-xywh element-dimensions element-offset workspace->doc]])
   (:import [goog.ui IdGenerator]
           [goog.events EventType]))





(declare draggable)

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
   (fn [_] (sug/private! owner :count 0))
   :render-state
   (fn [_ state]
     (let [renders (sug/private owner :count)]
     (sug/private! owner :count (inc renders))
     (dom/div #js {:className "render-counter"}
        (dom/p nil (str (inc renders) )))))})


(defn bool-box [data owner opts]
  (reify
    om/IRender
    (render [_]
      (let [text (or (:text data) "")
            value (:value data)]

       (dom/label #js {:className "bool"}
           (dom/p nil text)
           (dom/input #js {:type "checkbox"
                           :checked (:value data)
                           :onChange #(om/transact! data [:value] not)}))))))


(sug/defcomp icon
  [data owner opts]
  {:render-state
    (fn [_ state]
      (let [class-name (or (:className opts) "")
            on-click (or (:onClick opts) (fn [e] ))
            sheet (or (:sheet opts) "url(img/views.png)")
            [w h] [(or (:w opts) 16) (or (:h opts) 16)]
            [x y] [(or (:x state) 0) (or (:y state) 0)]
           o (str (px x) " " (px y))]
        (dom/div #js {:className (str "icon " class-name)
                      :onClick on-click
                      :style #js {:background-image sheet
                                  :width (px w) :height (px h)
                                  :background-position o}} "")))})

(sug/defcomp drop-down
  [data owner opts]
  {:render-state
    (fn [_ state]
      (let [active (:view state)
            options (keys (:options opts))
            sorted (cons active (vec (disj (set options) active)))
            offsets (map #(apply str (interpose " " (map  px (get (:options opts) %)))) sorted) ]
      (apply dom/div #js {:className "icon-select"}
               (map (fn [v o] (dom/div #js {:value v
                                   :onClick #(sug/fire! owner :set-view {:view v})}
                              (dom/div #js {:className "icon"
                                            :style #js {:background-position o}} "") (str v)))
                sorted offsets))))})


(sug/defcomp modal-box [data owner opts]
  {:will-update
   (fn [_ next-props next-state]
                 )
   :render-state
   (fn [_ state]
      (let [row-count (count (:options data))
            cname (str "mode-box " (when (true? (:disabled state)) "disabled"))]
      (apply dom/div #js {:className cname}
           (for [ridx (range 0 row-count)
                 :let [row (nth (:options data) ridx)
                       amount (count row)]]
           (apply dom/div #js {:className "row"}
            (map-indexed (fn [idx part]
                   (let [left (= 0 idx)
                         right (= amount (inc idx))
                         top (= 0 ridx)
                         bottom (= row-count (inc ridx))
                         class-str (str "option "
                                        (when (= (:active data) part) "active ")
                                        (when (and top left) "top_left ")
                                        (when (and top right) "top_right ")
                                        (when (and bottom left) "bottom_left ")
                                        (when (and bottom right) "bottom_right ")
                                        (when left "left ")
                                        (when (not-any? true? [left (and top left) (and top right)
                                                        (and bottom left) (and bottom right)]) "middle "))]
                     (dom/span #js {:className class-str
                                    :style #js {:width (str (/ 100 amount ) "%")}
                                    :onClick #(click-mode-option data owner part)} part))) row))) )))})



(defn child-nodes [nodes]
  (loop [data nodes
         result []]
    (let [children (filter map? (flatten (mapv :children data)))
          uids (mapv :uid data)]
      (if-not (pos? (count children))
        (concat result uids)
        (recur children (concat result uids))))))



(into {} [])
 (sug/defcomp dom-node [data owner opts]
  {:render-state
   (fn [_ state]
            (let [node data
                  nodes (:nodes opts)

                  uid (:uid node)
                  token (uid nodes)
                  tag (:tag token)

                  targeted (= (:mouse-target state) uid)
                  selected ((:selection state) uid)
                  locked (:locked token)
                  hidden (:hidden token)
                  selected-class (str (when selected "selected ")

                                        "") ;(when targeted "targeted ")
                  node-class (str
                              "outliner_node "
                              (when hidden "hidden ")
                              (when locked "locked ")
                              (when (expanded? token) "open "))
                  exp-class (cond (children? token) (cond (expanded? token) "exp-box expanded" :else "exp-box collapsed") :else "exp-box")]
              (apply dom/div #js {:className node-class  :onClick (fn [e]
                                                                    (when-not hidden
                                                                    (sug/fire! owner :select-node {:uid uid})) false)}
                     (dom/span #js {:className (str "outliner_background " selected-class)}

                               (sug/make draggable data {:opts {:className "node-drag"}
                                                         :init-state {:drag-start :drag-nodes-start
                                                                      :drag :drag-nodes
                                                                      :drag-stop :drag-nodes-stop}
                                                         :state {:message {:selected selected}}}))

                     (dom/span nil
                               (dom/div #js {:className exp-class
                                             :onClick (fn [e] (when (:expanded @token)

                                                                (sug/fire! owner :collapsing-nodes

                                                                           {:target @token :nodes (child-nodes [@node])}))
                                                        (toggle-expansion e token owner opts) false)}))
                     (dom/span #js {:className (str "tag-name " tag)} tag)
                     (when (:id token) (dom/span #js {:className "id-name"} (:id token)))

                     (sug/make icon data {:state {:x (if hidden -64 -48) :y -16}
                                          :opts {:onClick #(sug/fire! owner :toggle-hide {:uid uid})}})
                     (sug/make icon data {:state {:x (if locked -48 -32) :y -96}
                                          :opts {:onClick #(sug/fire! owner :toggle-lock {:uid uid})}})
                     ;(sug/make render-count data {:state {:r (rand)}})

                     (when (and (children? token) ) ;(expanded? token))
                       (for [child (:children node)]
                         (sug/make dom-node child {:opts {:nodes nodes}
                                                   :state {:selection (:selection state)
                                                           :mouse-target (:mouse-target state)}}))
                       ))))})



(defn unbind-drag [owner]
  (let [[mouse-up mouse-move] (sug/private owner :event-handlers)]
    (.unbind (js/$ js/window) "mouseup.drag" mouse-up)
    (.unbind (js/$ js/window) "mousemove.drag" mouse-move)
    ))

(defn drag-stop [e data owner]
  (let [loc  (location e)]
    (sug/fire! owner (or (om/get-state owner :drag-stop) :drag-stop)
               (conj {:location loc
                      :start-location (sug/private owner :start-location)
                      :diff-location (map - loc (sug/private owner :last-location))} (om/get-state owner :message)))
    (unbind-drag owner)))

(defn drag [e data owner]
  (let [loc (if (= (aget e "origin") "iframe")
              (workspace->doc (location e))
              (location e))]

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
    (.bind (js/$ js/window) "mouseup.drag" mouse-up)
    (.bind (js/$ js/window) "mousemove.drag" mouse-move)

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

