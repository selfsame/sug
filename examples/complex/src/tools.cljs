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
   [examples.complex.util :only [value-from-node clear-nodes! location clog px style!
                                 to? from? within? get-xywh element-dimensions element-offset get-xywh]]
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

(sug/defcomp filtered-inline [data owner]
  {:render-state
   (fn [_ state]
       (let []
            (dom/p nil (prn-str (rand-int 100) data)) ))})

(sug/defcomp history [data owner]
  {:render-state
   (fn [_ state]
       (let []
         (dom/div nil
            (sug/make filtered-inline (:inline data) {})
            (dom/p nil (prn-str (rand-int 100) (:nodes data)))) ))})



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
             {:render-state
              (fn [_ state]

                (let [app (om/value data)
                      selected (:selection app)
                      nodes (:nodes app)
                      selected-nodes (vals (select-keys nodes selected))

                      select (:style-select data)
                      pseudo (:style-use-pseudo data)
                      pseudo-opts (:style-pseudo data)
                      css-rules (:css-rules CSS-INFO)]

                  (dom/div #js {:className (str "options" (when (= "create" (:active (:mode data))) " disabled"))}
                           (sug/make modal-box select {})
                           (sug/make bool-box pseudo {})
                           (sug/make modal-box pseudo-opts {:state {:disabled (not (:value pseudo))}})
                           (apply dom/div #js {:className "rules"}

                                  (map (fn [rule]
                                         (sug/make widgets/style-widget (:selection data)
                                                   {:init-state {:rule rule}
                                                    :state {:selected-nodes selected-nodes}})) css-rules)
                                  ))))})

(defn tool-lookup [view data]
  (let [comps {:mode mode :history history :outliner outliner :style style}
        lenses {:mode (:app-state data)
                :history (:filtered (:app-state data))
                :outliner (:app-state data)
                :style (:app-state data)}]
    [(view comps) (view lenses)]))


;(not= (om/get-render-state owner)
 ;          (next-state))


(sug/defcomp toolbox [data owner opts]
  {:render-state
    (fn [_ state]

       (let [view (:view state)
             tabbed (:tabbed state)
             style (if (:docked state)
                     #js {:height (px (:height state))}
                     #js {:height (px (:height state)) :width (px (:width state))
                          :top (px (:top state)) :left (px (:left state))})]


         (dom/div #js {:className "tool" :ref "tool" :style style}
            (sug/make draggable data {:opts {:className (str "title " (when tabbed "tabbed "))
                                            :content (str view)} ;(aget owner "_rootNodeID")) }

                                     :init-state {:message {:idx (:idx state)}
                                                  :drag-start :remember
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
                                     :init-state {:drag-start :remember
                                                  :drag :drag-resize}})))))


   :on {:remember (fn [e]
                    (let [el-off (element-offset (om/get-node owner "tool"))
                          el-dim (element-dimensions (om/get-node owner "tool"))
                          drag-start (:start-location e)]
                    (om/set-state! owner :drag-start-offset
                                   (mapv - drag-start el-off))
                      (om/set-state! owner :drag-start-offset-dim
                                   (mapv - drag-start el-dim))))
        :drag-free
        (fn [e]
          (let [state (om/get-state owner)
                [x y] (mapv - (:location e) (state :drag-start-offset ))
                node (om/get-node owner "tool")]
            (style! node :left (px x))
            (style! node :top (px y))))

        :drag-resize (fn [e]
                       (let [state (om/get-state owner)
                             node (om/get-node owner "tool")
                             [nw nh] (mapv - (:location e) (state :drag-start-offset-dim ))]
                         (style! node :width (px nw))
                         (style! node :height (px nh))))}})


