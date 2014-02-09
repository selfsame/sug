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
   [goog.events :as events])
  (:use

    [examples.complex.data :only [UID GLOBAL INTERFACE]]
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

  {:init-state
   (fn [_]

       (let [shelf (om/value data)
             window (om/get-shared owner :window)
             wh @(:height window)
             h (- wh 24)
             result (vec (map (fn [v] (* h v)) (:spacing shelf)))]
                {:spacing result
                 :stack (:stack shelf)}))

  :render-state
  (fn [_ state]
      (let [stack (:stack data)
            align (om/value (:align data))
            style (if (:right state)
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
                           (sug/make tools/toolbox tool {:state {:uid idx
                                                                 :docked true
                                                                 :height (px  tool-height)}})))


                  (let [handle-style (if (:right state)
                    #js {:right (px (:width state)) :top (px (:top state))}
                    #js {:left (px (:width state)) :top (px (:top state))})]
                 (sug/make draggable data {:opts {:className "handle"
                                                   :style handle-style}
                                            :state {:drag (keyword (str "drag-shelf" align))}})))))

   :on {:drag-docked
        (fn [e]
          (let [state (om/get-state owner)
                spacing (:spacing state)
                uid (:uid e)
                diff (:diff-location e)
                d-y (- (last diff))
                slid (slide-in-stack uid spacing d-y)]
            (om/set-state! owner :spacing slid)))}})






(sug/defcomp undocked-tools
  [data owner opts]

  {:render-state
    (fn [_ state]
      (let []
      (dom/div #js {:className "undocked"}
        (into-array
          (map (fn [tool]
                 (let [[left top width height] (:xywh tool)]
                 (sug/make tools/toolbox tool {:init-state {:uid -1
                                                            :docked false
                                                            :left left :top top
                                                            :width width :height height} }))) (:stack data))))))})




(sug/defcomp interface
  [data owner opts]
  {:will-mount
    (fn [_]

      (let [window (om/get-shared owner :window)
            ww @(:width window)]
        (om/transact! data :layout #(map * % [ww ww ww]))))
  :will-update
   (fn [_ _ next-state]
     (let [old (:app-state (:root (om/get-render-state owner)))
           new (:app-state (:root next-state))
           diffs   (vec (filter (fn [k] (when  (not= (k old) (k new)) k)) (keys new)))]
      (when-not (empty? diffs)
        (do (prn diffs)

        (sug/fire-down! owner diffs {:is true})) )))

  :render-state
    (fn [_ state]
      (let [window (om/get-shared owner :window)
            ww @(:width window)
            wh @(:height window)
            layout (:layout data)]
      (om/set-state! owner :root (:_root opts))

      (aset (.-style (.getElementById js/document "workspace")) "left" (px (+ 8 (first layout)) ))
      (aset (.-style (.getElementById js/document "workspace")) "width" (px (- (nth layout 1) 24) ))
      (aset (.-style (.getElementById js/document "workspace")) "top" (px 24) )

      (dom/div nil
        (sug/make menubar (:menubar data) {})
        (sug/make tool-shelf (:left-shelf data) {:state {:width (first layout) :left 0 :top 24}} )
        (sug/make tool-shelf (:right-shelf data) {:state {:width (last layout) :right 0 :top 24}} )
        (sug/make undocked-tools (:undocked data) {})

               )))

   :on {
       :drag-shelf:left (fn [e]
                         (let [newpos (:diff-location e)
                               xd (first newpos)]
                         (om/transact! data :layout #(map + % [xd (- xd) 0]))))
        :drag-shelf:right (fn [e]
                         (let [newpos (:diff-location e)
                               xd (first newpos)]
                         (om/transact! data :layout #(map + % [0 xd (- xd)]))))
        :selection #()
        :mode #()
        :mouse-target #()
         }})

(defn toggle [col v]
  (if (col v) (disj col v)
    (conj col v)))


(defn check-mousedown [e data owner]
  (let [target (.-target e)
        uid (.-uid target)]
    (om/transact! data [:app-state :selection] #(toggle % uid))))

(defn check-mousemove [e data owner]
  (let [target (.-target e)
        uid (.-uid target)]
    (om/transact! data [:app-state :mouse-target] #(identity uid))))

(sug/defcomp dom-app
  [data owner opts]
  {:will-mount
    (fn [_]
      (doto (domina/by-id "workspace")
            (events/listen EventType.MOUSEDOWN #(check-mousedown % data owner))
            (events/listen EventType.MOUSEMOVE #(check-mousemove % data owner))))
   :will-update
   (fn [_ _ next-state] )
   :render
    (fn [_]


        (om/build interface (:interface data) {:opts {:_root data} :state {:a (rand)}})) ;:state {:_dirty (om/get-state owner :_dirty)}}))
   :did-update
   (fn [_ _ _ _]
     )

   })

(def DATA (atom (merge-with conj INTERFACE {:app-state {
                                                        :dom [(tokenize/make (domina/by-id "workspace"))]
                                                        :nodes @tokenize/NODES}})))

(def SHARED (merge GLOBAL {:window {:width (atom (.-innerWidth js/window))
                                      :height (atom (.-innerHeight js/window))}}))





(:nodes (:app-state @DATA))

(om/root
 DATA
 SHARED
 dom-app (.getElementById js/document "main"))



