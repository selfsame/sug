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

       (let [app-state data]
         (dom/div nil
           (sug/make modal-box (:mode app-state) {})
           (sug/make modal-box (:element-filter app-state) {})
                  ) ))})

(defn history [app owner opts]
  (reify
    om/IRender
    (render [_]
       (let []
            (dom/p #js {:style #js {}} ) ))))

(sug/defcomp outliner [data owner opts]
  {
   :render-state
   (fn [_ state]
      (let [app-state data
            selection (:selection app-state)
            mouse-target (:mouse-target app-state)
            cname (str "select-" (:active (:element-filter app-state)))]
        (apply dom/div #js {:className cname :id "outliner"}
          (sug/make-all dom-node (:dom app-state) {:state {:selection selection :mouse-target mouse-target} }))))})


(sug/defcomp style [data owner opts]
  {
   :render-state
   (fn [_ state]

       (let [app data
             selected (:selection app)
             nodes (:nodes app)
             style-set (apply conj (map :style (vals (select-keys nodes selected))))
             select (:style-select app)
             pseudo (:style-use-pseudo app)
             pseudo-opts (:style-pseudo app)
             css-rules (:css-rules CSS-INFO)]

         (dom/div #js {:className (str "options" (when (= "create" (:active (:mode app))) " disabled"))}
            (sug/make modal-box select {})
            (sug/make bool-box pseudo {})
            (sug/make modal-box pseudo-opts {:state {:disabled (not (:value pseudo))}})
            (apply dom/div #js {:className "rules"}

              (map (fn [rule]
                (sug/make widgets/style-widget data {:state {:styles style-set
                                                                        :rule rule}})) css-rules)
                ))))})

(defn tool-lookup [view data]
  (let [comps {:mode mode :history history :outliner outliner :style style}
        lenses {:mode (:app-state data)
                :history data
                :outliner (:app-state data)
                :style (:app-state data)}]
    [(view comps) (view lenses)]))


;(not= (om/get-render-state owner)
 ;          (next-state))


(sug/defcomp toolbox [data owner opts]
  {:should-update
   (fn [_ next-props next-state]

     (if (or
      (or (nil? next-state)
          (not= (om/get-state owner) next-state)
          (not= (om/get-render-state owner) (om/get-state owner)))
      (not= (om/get-props owner) next-props))
     true
     (prn "FALSE")))
   :render-state
    (fn [_ state]

       (let [view (:view state)
             tabbed (:tabbed state)
             style (if (:docked state)
                     #js {:height (px (:height state))}
                     #js {:height (px (:height state)) :width (px (:width state))
                          :top (px (:top state)) :left (px (:left state))})]


         (dom/div #js {:className "tool" :ref "tool" :style style}
            (sug/make draggable data {:opts {:className (str "title " (when tabbed "tabbed "))
                                            :content (str view (rand-int 100)) }

                                     :init-state {:message {:idx (:idx state)}
                                                  :drag (if (:docked state) :drag-docked :drag-free)}})
            (when tabbed
              (apply dom/div #js {:className "tabs"}
                       (map (fn [tab] (dom/div #js {:className (str "label " (when (= view tab) "active "))
                                           :onClick #(om/set-state! owner :view tab) } (dom/p nil (str tab)))) tabbed)))

            (dom/div #js {:className "view"
                          :id (if (= view :outliner) "outliner_view" "")}
              (when-let [[component lense] (tool-lookup view data)]
                (sug/make component lense {})))

            (when-not (:docked state)
              (sug/make draggable data {:opts {:className "resize"}
                                     :init-state {:drag :drag-resize}})))))


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

        }})


