(ns examples.complex.tools
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
   [examples.complex.widgets :as widgets]
   [examples.complex.final :as final]
   [cljs.core.async :as async :refer [>! <! put! chan]]
      )
  (:use

    [examples.complex.data :only [UID CSS-INFO KEYS-DOWN]]
   [examples.complex.tokenize :only [tokenize-style]]
   [examples.complex.util :only [value-from-node clear-nodes! location clog px style! jq-dimensions toggle
                                 to? from? within? get-xywh element-dimensions element-offset get-xywh]]
    [examples.complex.components :only [modal-box dom-node draggable bool-box render-count]]))


(enable-console-print!)

(defn dom-to-xywh [dom]
  (let [node (:node dom)
        bg (or (.-backgroundColor (.-style node)) "none")
        tag (:tag dom)
        [x y] (element-offset node)
        [w h] (element-dimensions node)]
  {:xywh [x y w h] :children (:children dom) :bg bg}))

(sug/defcomp mini-dom [data owner]
  {:render-state
   (fn [_ state]
     (let [dom (:dom state)
           depth (:depth state)
           children (:children dom)
           [x y w h] (:xywh dom)]

     (apply dom/div #js {:className "box"
                   :style #js {:background-color (:bg (:dom state)) :left (px x) :top (px y) :width (px w) :height (px h)}}

              (when (and (not (empty? children)) (pos? depth))
                (map #(sug/make mini-dom data {:init-state {:depth (dec depth)
                                                      :dom (dom-to-xywh %) }}) children)
                ))))})

(sug/defcomp mini-map [data owner]
  {:init-state
   (fn [_] {:wh [100 100]})

   :render-state
   (fn [_ state]
       (let [p-wh (:wh state)
             body (first (mapv dom-to-xywh data))
             [ox oy ow oh] (:xywh body)
             ratio (or (sug/private owner :ratio) 1)]
         (sug/private! owner :workspace-dim [ow oh])
         (dom/div #js {:className "mini-map"}
            (dom/p #js {:className "title"} "demo.html")
            (dom/div #js {:className "preview"}
                     (dom/div #js {:ref "holder" :className "holder" :style #js {:zoom ratio}}
                         (sug/make mini-dom data {:init-state {:depth 3 :dom body}})))
            (dom/div #js {:className "viewport"} )
            (dom/div #js {:className "control"}
                  (dom/button #js {:className "zoom in"} "+")
                  (dom/button #js {:className "grab"} "g")
                  (dom/button #js {:className "draw-view"} "|_+")
                  (dom/button #js {:className "zoom out"} "-")))))
   :on {:tool-resize
        (fn [e]
          (let [[w h] (mapv - [(:w e) (:h e)] [30 118])
                [ow oh] (sug/private owner :workspace-dim)
                 ratio (apply min (mapv /  [w h] [ow oh]))
                holder (om/get-node owner "holder")]
            (sug/private! owner :ratio ratio)
            (when holder
              (style! holder :zoom ratio)) ))}})

(filter  (fn [v] (neg? (last v))) {:a 5 :b -3 :c -4})


(sug/defcomp node-box  [data owner]
  {:render-state
   (fn [_ state]
   (let [uid (:uid data)]
   (dom/div #js {:className "node-box"}
          (sug/make render-count data {:state {:r (rand)}})
          (dom/div #js {:className "head"}
                 (dom/div #js {:className "datum uid"}
                        (dom/p #js {:className "value"} (prn-str (:uid data)) )
                        (dom/p #js {:className "label"} ":uid"))
                 (dom/div #js {:className "datum tag"}
                        (dom/p #js {:className "value"} (prn-str (:tag data)))
                        (dom/p #js {:className "label"} ":tag"))
                 (dom/div #js {:className "datum expanded"}
                        (dom/p #js {:className "value"} (prn-str (:expanded data)))
                        (dom/p #js {:className "label"} ":expand?")))
          (dom/div #js {:className "tail"}
                 (dom/div #js {:className "datum inline"}
                        (dom/p #js {:className "value"} (prn-str (keys (:inline data)) ))
                        (dom/p #js {:className "label"} ":inline"))
                 (dom/div #js {:className "datum path"}
                        (dom/p #js {:className "value"} (prn-str (:uid-path data)))
                        (dom/p #js {:className "label"} ":path"))))

     ))})



(sug/defcomp filter-view  [data owner]
  {:render-state
   (fn [_ state]


       (apply dom/div nil
     (map (fn [k] (sug/make
            node-box
            (k (:nodes data)) {}))
          (keys (:nodes data))))
     )})
(count {:a 6})
(some false? [true true nil false nil])
(filter #(not(nil? %)) [true nil true true] )

(sug/defcomp checkbox [data owner]
    {:render-state
     (fn [_ state]
        (dom/label #js {:className "bool"}
           (dom/p nil (:text state))
           (dom/input #js {:type "checkbox"
                           :checked (:value state)
                           :onChange (fn [e]
                                       (sug/fire! owner :toggle {:type (:type state) :value (not (:value state))})
                                       (om/set-state! owner :value (not (:value state))))})))})


(sug/defcomp word-processor  [data owner]
             {:init-state
              (fn [_]
                {:filters {:expanded {:value true :text "expanded"}
                              :children {:value false :text "has children"}
                              :selected {:value false :text "selected"}
                              :styled {:value false :text "styled"}}})
              :render-state
              (fn [_ state]
                (let [filters (:filters state)
                      fkeys (set (filter (fn [k] (:value (k filters))) (keys filters)))
                      filt-fn (fn [entry]
                                (not (some false?
                                  [(when (:expanded fkeys)
                                    (if (and (:expanded entry)
                                             (pos? (count (:children entry)))) true false))
                                  (when (:children fkeys)
                                    (if (pos? (count (:children entry))) true false))
                                  (when (:selected fkeys)
                                    (if ((:uid entry) (:selection data)) true false))
                                  (when (:styled fkeys)
                                    (if (pos? (count (:inline entry))) true false))])))]
                (dom/div nil
                         (dom/div #js {:style #js {:width 300} :float :left}
                         (sug/make checkbox  data {:init-state (conj (:expanded filters) {:type :expanded})})
                         (sug/make checkbox  data {:init-state (conj (:children filters) {:type :children})})
                         (sug/make checkbox  data {:init-state (conj (:selected filters) {:type :selected})})
                         (sug/make checkbox  data {:init-state (conj (:styled filters) {:type :styled})}))

                         (sug/make
                          filter-view
                          data {:fn (fn [d]
                                      (update-in d [:nodes]
                                                 (fn [n]
                                                   (into {} (map (fn [a]
                                                                   (if (filt-fn a)
                                                                     {(:uid a) a}
                                                                     {})) (vals n)) ))))}))))
              :on {:toggle (fn [e]
                             (let [typ (:type e) value (:value e)]
                               (om/set-state! owner [:filters typ :value] value) ))}})

 (sug/defcomp mode [data owner opts]
  {:render-state
   (fn [_ state]

       (let [app-state data]
         (dom/div nil
           (sug/make modal-box (:mode app-state) {})
           (sug/make modal-box (:element-filter app-state) {})
                  ) ))})



(sug/defcomp history [data owner]
  {:render-state
   (fn [_ state]
       (let []
         (dom/div nil

            ;(sug/make filtered-inline (:selected-nodes data) {})
            (prn data)
                  )) )})


(defn select! [uid-set uid]
  (if (@KEYS-DOWN 16)
    (toggle uid-set uid)
    #{uid}))

(sug/defcomp outliner [data owner opts]
  {:render-state
   (fn [_ state]
      (let [app-state data
            selection (:selection app-state)
            mouse-target (:mouse-target app-state)
            cname (str "select-" (:active (:element-filter app-state)))]
        (apply dom/div #js {:className cname :id "outliner"}
          (sug/make-all dom-node (:dom app-state) {:state {:selection selection :mouse-target mouse-target} }))))
   :on {:select-node
        (fn [e] (om/transact! data [:selection] #(select! % (:uid e))))
        :collapsing-nodes
        (fn [e] (let [target (:target e)
                      uid (:uid target)
                      value (not (:expanded target))
                      nodes (:nodes e)]
                  (om/transact! data [:nodes uid :expanded] not)
                  (om/transact! data [:selection] #(apply disj % nodes) ))) }})



(sug/defcomp style [data owner opts]
             {:render-state
              (fn [_ state]

                (let [select (:style-select data)
                      pseudo (:style-use-pseudo data)
                      pseudo-opts (:style-pseudo data)
                      css-rules (:css-rules CSS-INFO)]

                  (dom/div #js {:className (str "options" (when (= "create" (:active (:mode data))) " disabled"))}
                           (sug/make modal-box select {})
                           (sug/make bool-box pseudo {})
                           (sug/make modal-box pseudo-opts {:state {:disabled (not (:value pseudo))}})
                           (apply dom/div #js {:className "rules"}
                                  (map (fn [rule]
                                         (sug/make widgets/style-widget   data
                                                   {:init-state {:rule rule}})) css-rules)))))
              :on {:style-change
                   (fn [e]
                     (let [rule (:rule e)
                           value (:value e)
                           uids (:selection @data)
                           node-data (vals (select-keys (:nodes @data) uids))]

                       (dorun
                        (map
                         (fn [entry]
                           (let [node (:el entry)
                                 uid (:uid entry)]
                           (aset (.-style node) rule value)
                           (final/update-selection @data)

                             ))  node-data))) )
                   :style-set-done
                   (fn [e]
                     (let [uids (:selection @data)
                           node-data (vals (select-keys (:nodes @data) uids))]

                       (dorun
                        (map
                         (fn [entry]
                           (let [node (:el entry)
                                 uid (:uid entry)]
                           (om/transact! data [:nodes uid :inline ] #(tokenize-style node)))) node-data)))) }})

(defn tool-lookup [view data]
  (let [comps {:mode mode :history history :outliner outliner
               :style style :mini-map mini-map :word-processor word-processor}
        lenses {:mode data
                :history data
                :outliner data
                :style data
                :mini-map (:dom data)
                :word-processor data}]
    [(view comps) (view lenses)]))




(sug/defcomp toolbox [data owner opts]
  {:did-mount
   (fn [_]
     (let [node (om/get-node owner)
           [w h] (element-dimensions node)]
     (sug/private! owner :element-dim [w h])
     (sug/fire-down! owner :tool-resize {:w w :h h})))
   :render-state
    (fn [_ state]

       (let [view (:view state)
             tabbed (:tabbed state)
             [x y w h] (or (:xywh state) [(:left state) (:top state) (:width state) (:height state)])
             style (if (:docked state)
                     #js {:height (px h)}
                     #js {:height (px h) :width (px w) :top (px y) :left (px x)})]

         (dom/div #js {:className "tool" :ref "tool" :style style}
            (sug/make draggable data {:opts {:className (str "title " (when tabbed "tabbed "))
                                            :content (str view "  "  "  " (:uid state))} ;(aget owner "_rootNodeID")) }

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


   :on {:tool-resize (fn [e]) ;use this channel to broadcast resize to descendants

        :drag-free
        (fn [e]
          (let [node (om/get-node owner "tool")
                [x y] (element-offset node)
                [dx dy] (:diff-location e)
                [nx ny] (map + [x y] [dx dy])]
            ;(om/set-state! owner [:windows uid :xywh] new-xywh)
            (style! node :left (px nx))
            (style! node :top (px ny))
            ))

        :drag-resize (fn [e]
                       (let [node (om/get-node owner "tool")
                             [w h] (element-dimensions node)
                             [nw nh] (mapv + [w h] (:diff-location e))]

                         (style! node :width (px nw))
                         (style! node :height (px nh))))
        :resize-docked (fn [e]
                         (let [state (om/get-state owner)]
                           (when (:docked state)
                             (when (=(:align e) (:align state))
                               (when (= (:uid e) (:uid state))
                                 (let [node (om/get-node owner "tool")
                                       [w h] (sug/private owner :element-dim)
                                       dh (:dh e)]
                                   (sug/private! owner :element-dim (element-dimensions node))
                                   (style! node :height (px (+ h dh)))
                                   (sug/private! owner :element-dim [w (+ h dh)])
                                   (sug/fire-down! owner :tool-resize {:w w :h (+ h dh)})
                                   ))))))  }})


