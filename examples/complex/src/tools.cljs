(ns examples.complex.tools
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
   [examples.complex.widgets :as widgets]
   [cljs.core.async :as async :refer [>! <! put! chan]]
      )
  (:use
    [examples.complex.data :only [UID INITIAL CSS-INFO]]
   [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within? get-xywh element-dimensions element-offset get-xywh]]
    [examples.complex.components :only [modal-box dom-node draggable bool-box]]))


(enable-console-print!)


 (sug/defcomp mode [data owner opts]
  {:render-state
   (fn [_ state]

       (let [app (:app-state (:_root opts))
             el-fil (:mode app)]
         (dom/div nil
           (sug/make modal-box el-fil {})
           (sug/make modal-box (:element-filter app) {})) ))
   :on {:mode

        (fn [e] (prn "MODE")
          (om/set-state! owner :__c (rand)) ) }})

(defn history [app owner opts]
  (reify
    om/IRender
    (render [_]
       (let []
            (dom/p nil "unimplemented") ))))



 (sug/defcomp outliner [data owner opts]
  {:render-state
   (fn [_ state]
      (let [app (:app-state (:_root opts))
            cname (str "select-" (:active (:element-filter app)))]
        (apply dom/div #js {:className cname
                      :id "outliner"}
          (sug/make-all dom-node (:dom app) {})))  )
   :on {:selection
        (fn [e] (om/set-state! owner :__c (rand)) ) }})



(sug/defcomp style [data owner opts]
  {:render-state
   (fn [_ state]
       (let [app (:app-state (:_root opts))
             selected (:selection app)
             nodes (:nodes app)
             style-set (apply conj (map :style (vals (select-keys nodes selected))))
             select (:style-select app)
             pseudo (:style-use-pseudo app)
             pseudo-opts (:style-pseudo app)
             css-rules (:css-rules app)]

         (apply dom/div #js {:className (str "options" (when (= "create" (:active (:mode app))) " disabled"))}
            (sug/make modal-box select {})
            (sug/make bool-box pseudo {})
            (sug/make modal-box pseudo-opts {:state {:disabled (not (:value pseudo))}} )
            (sug/make-all widgets/style-widget css-rules {:state {:styles style-set}}))
                  ))
   :on {:selection
        (fn [e] (om/set-state! owner :__c (rand)) ) }})

(def tool-lookup {:mode mode :history history :style style :outliner outliner});{:mode mode   :style style})



(sug/defcomp toolbox [data owner opts]
  {:init-state
    (fn [_]
      {:rendered 0})

   :render-state
    (fn [_ state]

       (let [uid (:uid state)
             view (:view data)
             tabbed (:tabbed data)
             style (if (:docked state)
                     #js {:height (px (:height state))}
                     #js {:height (px (:height state)) :width (px (:width state))
                          :top (px (:top state)) :left (px (:left state))})]


         (dom/div #js {:className "tool" :ref "tool" :style style}
            (sug/make draggable data {:opts {:className (str "title " (when tabbed "tabbed "))
                                            :content (when-not tabbed (str view " " (:_dirty state))) }
                                     :state {:uid uid}
                                     :init-state {:uid uid
                                                  :drag (if (:docked state) :drag-docked :drag-free)}})
            (when tabbed
              (apply dom/div #js {:className "tabs"}
                       (map (fn [tab] (dom/div #js {:className (str "label " (when (= view tab) "active "))
                                           :onClick #(om/transact! data :view (fn [] tab)) } (dom/p nil (str tab)))) tabbed)))

            (dom/div #js {:className "view"
                          :id (if (= view :outliner) "outliner_view" "")}
              (when (tool-lookup view)
                (sug/make (tool-lookup view) data {})))

            (when-not (:docked state)
              (sug/make draggable data {:opts {:className "resize"}
                                     :state {:uid uid}
                                     :init-state {:uid uid :drag :drag-resize}})))))


   :on {:drag-free
        (fn [e]
          (let [state (om/get-state owner)
                [dx dy] (:diff-location e)]
            (om/set-state! owner :left (+ dx (:left state)))
            (om/set-state! owner :top (+ dy (:top state)))))

        :drag-resize (fn [e]
                       (let [state (om/get-state owner)
                             wh [(:width state) (:height state)]
                             [nw nh] (map + wh (:diff-location e))]
                         (om/set-state! owner :width nw)
                         (om/set-state! owner :height nh)))
        :change
          (fn [e] (prn "ct") )
        }})


