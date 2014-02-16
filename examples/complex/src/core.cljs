(ns examples.complex.core
  (:require
      [domina]
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]
      [examples.complex.tokenize :as tokenize]
   [examples.complex.commands :as commands]
   [examples.complex.tools :as tools]
   [examples.complex.final :as final]
   [goog.events :as events])
  (:use

    [examples.complex.data :only [UID GLOBAL INTERFACE MOUSE-TARGET MESS]]
    [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within?
                     get-xywh element-dimensions element-offset exclude]]
    [examples.complex.components :only [modal-box dom-node draggable]])
  (:import
   [goog.events EventType]))

(enable-console-print!)

(defn put-local [k v]
  (.setItem (aget  js/window "localStorage") k v))

(defn get-local [k]
  (.getItem (aget  js/window "localStorage") k ))

(defn _t []
  (aget js/window "_t"))

(defn position-native [left top width height]
  (aset (.-_m js/window) "outer_x" left)
  (aset (.-_m js/window) "outer_y" top)
  (aset (.-_m js/window) "outer_w" width)
  (aset (.-_m js/window) "outer_h" height)

  (for [id ["iframe_proxy" "canvas_clip"]]
    (let [rule (if (= "iframe_proxy" id) 16 0)]
    (do
    (aset (.-style (.getElementById js/document id)) "left" (px (+ left rule) ))
      (aset (.-style (.getElementById js/document id)) "width" (px (- width rule) ))
      (aset (.-style (.getElementById js/document id)) "height" (px (- height rule) ))
      (aset (.-style (.getElementById js/document id)) "top" (px (+ top rule)) )))))


(defn idx-for [v coll]
  (keep-indexed #(if (= %2 v) %1 nil) coll))



(sug/defcomp menu-option
  [data owner opts]
  {:render
    (fn [_]
      (let [k (om/value (:key data))]
      (dom/div #js {:className "menu_option"
                    :onClick #(commands/route-menu k)}
         (dom/span nil (:name data))
         (dom/span #js {:className "binding"} (:key-bind data)))))})



(sug/defcomp menubar
  [data owner opts]

  {:render
    (fn [_]
      (dom/div #js {:className "menubar"}
           (into-array
            (map (fn [k]
                   (dom/div #js {:className "menu"}
                      (dom/p nil (str k))
                        (apply dom/div #js {:className "menu_box"}
                            (sug/make-all menu-option (k data) {} ))))
                 (keys data) ))))})


(sug/defcomp document
  [data owner opts]
  {:render-state
    (fn [_ state]
      (let [box (:box data)]
      (dom/div #js {:className "document"
                    :style #js {:left (px (:left state))
                                :height (px (:height box))
                                :width (px (:width state))}} )))})

(defn dissort [col k]
  (zipmap (keys (dissoc col k)) (iterate inc 0)))



(defn undock-tool [data owner opts e]
  (let [state (om/get-state owner)
        side (:side state)
        uid (:uid e)
        stack (:stack state)
        id (first (idx-for uid stack))
        box (:box e)
        {:keys [left top width height]} box]
    (om/set-state! owner :spacing (exclude (:spacing state) id))
    (om/set-state! owner :stack (exclude (:stack state)   id))
    (om/transact! data [:interface side :stack] #(exclude % id))
    (om/transact! data [:interface side :spacing] #(exclude % id))
    (om/transact! data [:interface :undocked :stack] #(conj % uid))
    (om/transact! data [:interface :tool-boxes uid] #(conj % {:docked false
                                                              :fire-listener {:start-location (:location e)
                                                                              :last-location (:location e)}}))
    (om/transact! data [:interface :tool-boxes uid :box] #(merge % {:width width
                                                                    :height height
                                                                    :left left
                                                                    :top top} ))))




(defn slide-in-stack [idx col v]
  (let [len (dec (count col))]
    (vec (map-indexed (fn [index item]
                        (cond
                         (= index idx) (+ item v)
                         (= index (dec idx)) (- item v)
                         :else item)) col))))



(sug/defcomp tool-shelf
  [data owner opts]

  {:will-mount
   (fn [_]
     (let [state (om/get-state owner)
           h (:height state)]
     (om/set-state! owner :spacing (mapv #(* % h) (:spacing (:shelf state))))))

  :render-state
  (fn [_ state]
      (let [shelf (:shelf state)
            stack (:stack shelf)
            align (:align shelf)
            style (if (= :right align)
                    #js {:top (:top state) :right (px (:right state)) :width (px  (:width state))}
                    #js {:top (:top state) :left (px (:left state)) :width (px (:width state))})]

;;         (when-let [e (:trigger-undock state)]
;;           (when e
;;             (if ((set stack) (:uid e))
;;               (do
;;                 (om/set-state! owner :trigger-undock nil)
;;                 (undock-tool data owner opts e)))))


        (dom/div #js {:className "shelf" :ref "shelf" :style style}
               (apply dom/div nil
                 (for [idx (range 0 (count stack))
                        :let [tool (nth stack idx)
                              tool-height (nth (:spacing state) idx)]]

                           (sug/make tools/toolbox data {:init-state {:idx idx
                                                                      :view (:view tool)
                                                                 :tabbed (:tabbed tool)
                                                                 :docked true}
                                                         :state {:idx idx
                                                                 :height tool-height}})))


                  (let [handle-style (if (= :right align)
                    #js {:right (px (:width state)) :top (px (:top state))}
                    #js {:left (px (:width state)) :top (px (:top state))})]
                 (sug/make draggable data {:opts {:className "handle"
                                                   :style handle-style}
                                            :state {:drag-start :remember-shelf
                                                    :drag (keyword (str "drag-shelf" align))}})))))

   :on {:drag-docked
        (fn [e]
          (let [state (om/get-state owner)
                spacing (:spacing state)
                idx (:idx e)
                diff (:diff-location e)
                d-y (- (last diff))
                slid (slide-in-stack idx spacing d-y)]
            (print idx)
            (om/set-state! owner :spacing slid)))}})






(sug/defcomp undocked-tools
  [data owner opts]

  {:render-state
    (fn [_ state]
      (let [tools (:undocked (:interface  data))]

      (dom/div #js {:className "undocked"}
        (into-array
          (map (fn [tool]
                 (let [view (:view tool)
                       [left top width height] (:xywh tool)]
                 (sug/make tools/toolbox data {:key :uid :init-state {:view view
                                                            :docked false
                                                            :tabbed (:tabbed tool)
                                                            :left left :top top
                                                            :width width :height height} }))) tools )))))})


(sug/defcomp interface
  [data owner opts]
  {:will-mount
    (fn [_]

      (let [window (om/get-shared owner :window)
            ww @(:width window)]
        (om/set-state! owner :layout (map * (:layout (:interface data)) [ww ww ww]))))
  :will-update
   (fn [_ next-props next-state]
     (let [state (om/get-render-state owner)
           window (om/get-shared owner :window)
            ww @(:width window)
            wh @(:height window)
            layout (:layout state)]
       (when (or
              (not=
               (:selection (:app-state data))
               (:selection (:app-state next-props)))
              (not= (:layout state) (:layout next-state)))
         (final/update-selection next-props))
       ))
  :render
    (fn [_]
      (let [state (om/get-state owner)
            window (om/get-shared owner :window)
            ww @(:width window)
            wh @(:height window)
            layout (:layout state)]
      (let
        [left (+ 8 (first layout))
         top 24
         right (+ (nth layout 2) 8)
         width (- ww (+ left right))
         height (- wh top)]
          (dorun (position-native left top width height )))


      (dom/div nil
        (sug/make menubar (:menubar (:interface data)) {})
        (sug/make tool-shelf data (conj {:init-state {:shelf (:left-shelf (:interface data))
                                                :width (first layout) :height (- wh 24) :left 0 :top 24}}
                                   {:state {:width (first layout)}}) )
        (sug/make tool-shelf data {:init-state {:shelf (:right-shelf (:interface data))
                                                :width (last layout) :height (- wh 24) :right 0 :top 24}
                                   :state {:width (last layout)}} )
        (sug/make undocked-tools data {})

               )))

   :on {
        :remember-shelf (fn [e]
                          (om/set-state! owner :layout-remember (om/get-state owner :layout) ))
       :drag-shelf:left (fn [e]
                         (let [startpos (:start-location e)
                               newpos (:location e)
                               diffpos (map - newpos startpos)
                               remember (om/get-state owner :layout-remember)
                               xd (first diffpos)]
                         (om/set-state! owner :layout (map + (om/get-state owner :layout-remember) [xd (- xd) 0]))))
        :drag-shelf:right (fn [e]
                         (let [startpos (:start-location e)
                               newpos (:location e)
                               diffpos (map - newpos startpos)
                               remember (om/get-state owner :layout-remember)
                               xd (first diffpos)]
                         (om/set-state! owner :layout (map + (om/get-state owner :layout-remember) [0 xd (- xd)]))))
         }})

(defn toggle [col v]
  (if (col v) (disj col v)
    (conj col v)))



(defn drill-uid-path [node path]
  (let [next (first (filter #(= (:uid %) (first path)) (:children node)))]
    (if next
      (cond (= 1 (count path)) next
            :else (drill-uid-path next (rest path)))
      (:uid node))
  ))


(defn check-mousedown [e data owner]
  (let [target (.-target e)
        uid (.-uid target)]
    (om/transact! data [:app-state :selection] #(toggle % uid))))

(defn check-mousemove [e data owner]
  (let [target (.-target e)
        uid (.-uid target)
        uid-path (.-uid_path target)]

    (when-not (= (:mouse-target (:app-state @data)) uid)

      (swap! MOUSE-TARGET #(identity uid))
      (aset target "target" true)
      ;(om/transact! data [:app-state :mouse-target] #(identity uid))
      ;(om/transact! data [:app-state :mouse-target-path] #(identity
      ;                                                     (drill-uid-path (first (:dom (:app-state @data))) (rest uid-path))))
      )
  ))

(vals {})

(defn filter-data [data]
  "filters data depending on the current selection"
  (let [app (:app-state data)
        nodes (:nodes app)
        selection (:selection app)
        filtered (select-keys nodes selection)
        inline (or (apply merge (map :inline (vals filtered) )) {})]
    (update-in data [:app-state :filtered]
          (fn [x] {:inline inline
                   :nodes (or (vals filtered) [])}))))

(sug/defcomp dom-app
  [data owner opts]

  {:will-mount
   (fn [_]
      (doto (first (.toArray (js/$$ "body")))
            (events/listen EventType.MOUSEDOWN #(check-mousedown % data owner))
            (events/listen EventType.MOUSEMOVE #(check-mousemove % data owner))))

   :render
    (fn [_]
        (sug/make interface data {:fn filter-data}))})


(def DATA (atom (merge-with conj INTERFACE {:app-state {
                                                        :dom [(tokenize/make (first (.toArray (js/$$ "body"))) [])]
                                                        :nodes @tokenize/NODES}})))

(def SHARED (merge GLOBAL {:window {:width (atom (.-innerWidth js/window))
                                      :height (atom (.-innerHeight js/window))}}))




(js/$ (fn []


  (om/root
   DATA
   SHARED
   dom-app (.getElementById js/document "main"))

  (aset js/window "_m" #js {:window_w (.-innerWidth js/window)
                            :window_h (.-innerHeight js/window)
                            :scroll_x 0
                            :scroll_y 0
                            :outer_x 300
                            :outer_y 24
                            :outer_w 500
                            :outer_h 800
                            :iframe_w 0
                            :iframe_h 0
                            :ruler_w 16
                            :doc_scroll 0
                            :doc_w 0
                            :doc_h 0
                            })



  (.init (.-tracking (_t)))
  (.animate (.-tracking (_t)))))


