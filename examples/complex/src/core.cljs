(ns examples.complex.core
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]
      [examples.complex.tokenize :as tokenize]
   [examples.complex.commands :as commands]
   [examples.complex.tools :as tools])
  (:use
    [examples.complex.data :only [UID INITIAL]]
    [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within?
                     get-xywh element-dimensions element-offset exclude]]
    [examples.complex.components :only [modal-box dom-node draggable]]))

(enable-console-print!)

(defn put-local [k v]
  (.setItem (aget  js/window "localStorage") k v))

(defn get-local [k]
  (.getItem (aget  js/window "localStorage") k ))



(def DATA (atom (merge INITIAL {:dom [(tokenize/make (domina/by-id "data"))]})))


(defn docked? [k]
  (if (or (= :left k) (= :right k)) true false))


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
      (let [me (:thing opts)]
      (dom/div #js {:className "menubar"}
           (into-array
            (map (fn [k]
                   (dom/div #js {:className "menu"}
                      (dom/p nil (str k))
                        (apply dom/div #js {:className "menu_box"}
                            (sug/make-all menu-option (k (:menubar (:interface data))) {} ))))
                 (keys me) )))))})


(sug/defcomp document
  [data owner opts]
  {:render
    (fn [_]
      (let [left-shelf (:left-shelf (:interface data))
            right-shelf (:right-shelf (:interface data))]
      (dom/div #js {:className "document"
                    :style #js {:left (px (:width (:box left-shelf)))
                            :right (px (:width (:box right-shelf)))}} )))})

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


(defn tool-shelf-index [owner uid]
  (let [state (om/get-state owner)
        stack (:stack state)
        idx (first (idx-for uid stack))]
    idx))

(sug/defcomp tool-shelf
  [data owner opts]

  {:init-state
   (fn [_]
       (let [shelf (:thing opts)
             side (if (= :left (:align opts)) :left-shelf :right-shelf)
             spacing (om/value (:spacing shelf))
             h (:height (:window data))
             result (vec (map (fn [v] (* h v)) spacing))]
                {:side side
                 :spacing result
                 :stack (om/value (:stack shelf))
                 :box (om/value (:box shelf)) }))

  :render-state
  (fn [_ state]
      (let [shelf (:thing opts)
            box (:box state)
            stack (:stack state)
            side (:side state)
            style (if (:right box)
                    #js {:right (px (:right box)) :width (px  (:width box))}
                    #js {:left (px (:left box)) :width (px (:width box))})]

        (when-let [e (:trigger-undock state)]
          (when e
            (if ((set stack) (:uid e))
              (do
                (om/set-state! owner :trigger-undock nil)
                (undock-tool data owner opts e)))))

       (dom/div #js {:className "shelf" :ref "shelf" :style style}
               (dom/div nil
                 (into-array
                  (for [idx (range 0 (count stack))
                        :let [k (nth stack idx)
                              v idx]]
                           (sug/make tools/toolbox data {:opts {:uid k
                                                                :shelf side
                                                                :height (get (:spacing state) v )}
                                                         :init-state {:uid k}
                                                         :state {:uid k
                                                                 :height (get (:spacing state) v )}}))))

               (let [handle-style (if (:right box)
                    #js {:right (px (:width box)) :top (px (:top box))}
                    #js {:left (px (:width box)) :top (px (:top box))})]
                 (sug/make draggable shelf {:opts {:className "handle"
                                                   :style handle-style
                                                   :uid "shelf-handle" }
                                            :state {:uid "shelf-handle"}})))))
   :on {:drag-start (fn [e]
                      (om/set-state! owner :drag-target (:uid e)))
        :drag-stop (fn [e]
                      (om/set-state! owner :drag-target false))
        :drag (fn [e]
                (when (= (:uid e) "shelf-handle")
                  (do
                   (let [newpos (:diff-location e)
                         operator (if (= :left (:align opts)) + -)
                         side (if (= :left (:align opts)) :left-shelf :right-shelf)
                         new-width (operator (om/get-state owner [:box :width]) (int (first newpos)))]
                   (om/set-state! owner [:box :width] new-width)
                   (om/transact! data [:interface side :box :width] #(identity new-width))))))

        :docked-drag (fn [e] (let [side (if (= :left (:align opts)) :left-shelf :right-shelf)

                                   uid (:uid e)
                                   dif-y (last (:diff-location e))
                                   stack (om/get-state owner :stack)
                                   spacing (om/get-state owner :spacing)
                                   t (tool-shelf-index owner uid)
                                   shelf-el (om/get-node owner "shelf")
                                   shelf-xywh (get-xywh shelf-el)]

                               (if (and (= uid (om/get-state owner :drag-target)) ((set stack) uid))
                                 (do
                                   (when-not (within? shelf-xywh (:location e))
                                    (om/set-state! owner :trigger-undock e))

                                   (om/set-state! owner :spacing
                                     (vec (map-indexed
                                           (fn [idx itm]
                                             (cond
                                               (= t idx) (- itm dif-y)
                                               (= idx (dec t)) (+ itm dif-y)
                                               :else itm)) spacing)) )))))}})




(sug/defcomp undocked-tools
  [data owner opts]

  {:render-state
    (fn [_ state]
      (let [stack (:stack (:undocked (:interface data)))]
      (dom/div #js {:className "undocked"}
        (into-array
          (map (fn [uid]
                 (sug/make tools/toolbox data {:opts {:uid uid}
                                               :state {:uid uid} })) stack)))))})


(sug/defcomp interface
  [data owner opts]

  {:render-state
    (fn [_ state]
      (dom/div nil
        (into-array
         (for [k (keys (:interface data))]
           (let [thing (k (:interface data))]
           (cond
            (= :menubar k) (sug/make menubar data {  :opts {:thing thing :key k}})
            (= :shelf (:type thing)) (sug/make tool-shelf data {  :opts {:thing thing :key k :align (:align thing)}})
            (= :document (:type thing)) (sug/make document data { :opts {:thing thing :key k :align (:align thing)}})
            (= :undocked k ) (sug/make undocked-tools data { :opts {:thing thing :key k }})
            :else (dom/p nil (str (:type thing)))))))))})

(defn dom-app [app owner opts]
  (om/component
        (om/build interface app)))



(om/root
 (swap! DATA merge {:window {:width (.-innerWidth js/window) :height (.-innerHeight js/window)}})
 dom-app (.getElementById js/document "main"))


