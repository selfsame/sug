(ns examples.complex.tools
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true :refer [column row label group have havent config]]
   [examples.complex.widgets :as widgets]
   [examples.complex.final :as final]
   [cljs.core.async :as async :refer [>! <! put! chan]]
      )
  (:use
   [examples.complex.filebrowser :only [file-browser]]
   [examples.complex.data :only [UID CSS-INFO KEYS-DOWN MOUSE-DOWN-WORKSPACE MOUSE-TARGET
                                 OVER-HANDLE MOUSE-DOWN MOUSE-DOWN-POS MOUSE-POS SELECTION-BOX]]
   [examples.complex.tokenize :only [tokenize-style]]
   [examples.complex.util :only [value-from-node clear-nodes! location clog px style! jq-dimensions toggle
                                 to? from? within? get-xywh element-dimensions element-offset get-xywh]]
    [examples.complex.components :only [icon drop-down modal-box dom-node draggable bool-box render-count]]))


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







 (sug/defcomp mode [data owner opts]
  {:should-update
   (fn [this next-props next-state]
     (let [props (om/get-props owner)]
       (not (= (:app-state props)
               (:app-state next-props))))
     true)
   :will-mount
   (fn [_] (om/set-state! owner :custom (or (sug/private owner :custom ){})))
   :did-update
   (fn [_ _ _] (sug/private! owner :custom (om/get-state owner :custom)))
   :render-state
   (fn [_ state]

       (let [app-state (:app-state data)
             mode (:active (:mode app-state))
             wysiwyg (:wysiwyg app-state)]
         (dom/div nil
           (sug/make render-count app-state {:react-key (:uid state)})
           (if-not wysiwyg
             (sug/make modal-box (:mode app-state) {:opts {:classes "flooded "}})
             (sug/make modal-box data {:opts {:classes "flooded "
                                                           :active "wysiwyg"
                                                           :options [["wysiwyg"]]}}))
           (if wysiwyg
             (row (label "wysiwyg")
                    (dom/button #js {:onClick
                                     (fn [e]
                                       (let [el (:el (get (:nodes @app-state) (first (:selection @app-state))))]
                                          (om/transact! data [:app-state :wysiwyg] #(identity false))
                                          (.remove_focus (.-wysiwyg (.-tools js/window)) )
                                          (sug/fire! owner :dom-restructure {:root (first (:selection @app-state))})
                                         ))} "done"))
           (row
            (if (= "edit" mode)
             (row
              (config :draw-opts
                      (column 33
                              (label "options")
                              (sug/make bool-box (:show-margin (:edit-settings app-state)) {})
                              (sug/make bool-box (:show-padding (:edit-settings app-state)) {})))
              (config :alter-with (column 66
                      (label "alter-with")
                      (sug/make modal-box (:aspect (:edit-settings app-state)) {}))))
             (row
               (config :position
                       (column 33
                      (label "position")
                        (sug/make modal-box (:create-position (:create-settings app-state)) {})))
               (config :type
                       (column 66
                      (label "type")
                        (sug/make modal-box (:create-type (:create-settings app-state)) {})))))

             (when (= 1 (count (:selection app-state)))
               (row (label "wysiwyg")
                    (dom/button #js {:onClick
                                     (fn [e]
                                       (let [el (:el (get (:nodes @app-state) (first (:selection @app-state))))]
                                          (om/transact! data [:app-state :wysiwyg] #(identity el))
                                          (.focus_on (.-wysiwyg (.-tools js/window)) (js/$ el)) ))} "start")
                    )))
                  ))))})



 (sug/defcomp options [data owner opts]
  {:should-update
   (fn [this next-props next-state]
     (let [props (om/get-props owner)]
       (not (= (:app-state props)
               (:app-state next-props)))
       true))
   :will-mount
   (fn [_] (om/set-state! owner :custom (or (sug/private owner :custom ){})))
   :did-update
   (fn [_ _ _] (sug/private! owner :custom (om/get-state owner :custom)))
   :render-state
   (fn [_ state]

       (let [app-state (:app-state data)
             rulers (:rulers (:options app-state))]

             (row
              (config :rulers
              (column 33
                      (label "rulers")
                      (sug/make bool-box (:show rulers) {})
                      (sug/make bool-box (:show-guides rulers) {})
                      (sug/make bool-box (:snap-guides rulers) {})))
              (config :interface
                (column 66
                      (label "interface")
                      )))))
             })

(sug/defcomp multi [data owner]
  {:render-state
   (fn [_ state] (dom/p nil (prn-str data)))})


(sug/defcomp history [data owner]
  {:should-update
   (fn [this next-props next-state]
     (let [props (om/get-props owner)]
       (not (= (:app-state props)
               (:app-state next-props)))))
   :render-state
   (fn [_ state]
       (let [app-state (:app-state data)]
         (dom/div nil
            (sug/make render-count (:app-state data) {:react-key (:uid state)})
            (dom/code nil
                      (dom/button #js {:onClick #(sug/fire! owner :save-state {})} "save state")
                      (sug/make multi {:selection (:selection app-state) :mouse-target (:mouse-target app-state)} {})
                      ) )))})







(defn select! [uid-set uid]
  (if (@KEYS-DOWN 16)
    (toggle uid-set uid)
    #{uid}))

(sug/defcomp outliner [data owner opts]
  {:init-state
   (fn [_] {:filter #{"all"}})
   :should-update
   (fn [this next-props next-state]
     (let [props (om/get-props owner)]
       (not (= (:app-state props)
               (:app-state next-props)))))
   :render-state
   (fn [_ state]
      (let [app-state (:app-state data)
            selection (:selection app-state)
            mouse-target (:mouse-target app-state)
            cname (str "select-" (:active (:element-filter app-state)) " " (when (:moving state) "moving"))]
        (dom/div nil
                 (dom/div #js {:className "fixed-header"}
             (sug/make modal-box data {:state {:active (:filter state)
                                               :options [["all" "div" "img" "body" "p" "h1" "h2"]]
                                               :onChange (fn [a] (om/update-state! owner [:filter] #(toggle %1 a)))}}))

        (apply dom/div #js {:className cname :id "outliner"}
          (sug/make render-count app-state {:react-key (:uid state)})
           (for [child (:dom app-state)]
             (sug/make dom-node {:dom child
                                 :selection (:selection app-state)
                                 :mouse-target (:mouse-target app-state)
                                 :nodes (:nodes app-state)}
                                {:opts {}
                                 :init-state {:filter #{"all"}}
                                       ;:state {:filter (:filter state)
                                       ;        :selection (:selection app-state)
                                       ;        :mouse-target (:mouse-target app-state)}
                                 }))))))
   :on {:select-node
        (fn [e] (let [uid (:uid e)
                      nodes (:nodes (:app-state @data))
                      node (get nodes uid)
                      uid-path (:uid-path node)
                      locked (filter :locked (map #(get nodes %) uid-path))]
                  (when (empty? locked)
                      (om/transact! data [:app-state :selection] #(select! % (:uid e))))))
        :collapsing-nodes
        (fn [e] (let [target (:target e)
                      uid (:uid target)
                      value (not (:expanded target))
                      nodes (:nodes e)]
                  ;(om/transact! data [:app-state :nodes uid :expanded] not)
                  (om/transact! data [:app-state :selection] #(apply disj % nodes) )))
        :drag-nodes-start (fn [e]
                            (when (:selected e)
                              (final/set-cursor "n-resize")
                              (om/set-state! owner :moving true)))
        :drag-nodes (fn [e] )
        :drag-nodes-stop (fn [e]
                           (when (:selected e)
                           (final/set-cursor "default")
                           (om/set-state! owner :moving false)))
        :toggle-hide
        (fn [e] (let [uid (:uid e)
                      node (get (:nodes (:app-state @data)) uid)
                      el (:el node)]
                  (.toggleClass (js/$ el) "__hidden")
                  (om/transact! data [:app-state :nodes uid :hidden] not)))

        :toggle-lock
        (fn [e] (let [uid (:uid e)
                      node (get (:nodes (:app-state @data)) uid)
                      el (:el node)]
                  (.toggleClass (js/$ el) "__locked")
                  (om/transact! data [:app-state :nodes uid :locked] not)))
        }})



(sug/defcomp style [data owner opts]
             {:should-update
              (fn [this next-props next-state]
                (let [props (om/get-props owner)]
                  (not (= (:app-state props)
                          (:app-state next-props))))
                true)
              :will-mount
              (fn [_] (om/set-state! owner :custom (or (sug/private owner :custom ){})))
              :did-update
              (fn [_ _ _] (sug/private! owner :custom (om/get-state owner :custom)))
              :render-state
              (fn [_ state]

                (let [app-state (:app-state data)
                      select (:style-select app-state)
                      pseudo (:style-use-pseudo app-state)
                      target-breakpoint (:style-target-breakpoint app-state)
                      pseudo-opts (:style-pseudo app-state)
                      pseudo-element (:style-use-pseudo-element app-state)
                      css-rules (:css-rules CSS-INFO)
                      hide-options (or (:hide-options state) false)]

                  (dom/div #js {:className (str "options" (when (or
                                                                 (= "create" (:active (:mode app-state)))
                                                                 (:wysiwyg app-state)) " disabled"))}
                           ;(sug/make render-count data {:react-key (:uid state)})

;;                            (have :customize-tool (sug/make icon data {:opts {:className "config"
;;                                                                              :onClick #(om/update-state! owner [:custom :target] not )}
;;                                                                       :state {:x (+ 0 (have [:custom :target] -16))
;;                                                                               :y -80}}))
                           (config :target
                             (row (column 20
                                  (label "target"))
                                  (column 80
                                  (sug/make modal-box select {}))))

                           (config :advanced
                           (dom/div nil
                                    (dom/div #js {:className "dropdown" :onClick #(om/update-state! owner [:hide-options] not )}
                                             (sug/make icon data {:state {:x (if hide-options 0 -16) :y -96}})
                                             (dom/span nil "advanced"))

                                    (when-not hide-options
                                      (row
                                       (if (= "selection" (:active select))
                                         (row (group
                                               (column 66
                                                       (sug/make bool-box pseudo {})
                                                       (sug/make modal-box pseudo-opts {:state {:disabled (not (:value pseudo))}}))
                                               (column 33

                                                       (sug/make bool-box pseudo-element {})
                                                       (sug/make modal-box (:style-pseudo-element app-state) {:state {:disabled (not (:value pseudo-element))}})
                                                       (sug/make bool-box target-breakpoint {})
                                                       (dom/code nil "todo"))))
                                         (column 66
                                                 (label "css rules")))))))




                           (apply dom/div #js {:className "rules"}
                                  (map (fn [rule]
                                         (config (:name rule)
                                         (sug/make widgets/style-widget  data
                                                   {:fn (fn [col]
                                                          (let [pass1 (update-in col [:app-state] #(select-keys % [:selection :nodes]))]

                                                          (update-in pass1 [:app-state :nodes]
                                                                     #(select-keys % (:selection app-state)))))
                                                    :init-state {:rule rule}}))) css-rules)))))
              :on {:style-change
                   (fn [e]
                     (let [app-state (:app-state @data)
                           rule (:rule e)
                           value (:value e)
                           uids (:selection app-state)
                           node-data (vals (select-keys (:nodes app-state) uids))]

                       (dorun
                        (map
                         (fn [entry]
                           (let [node (:el entry)
                                 uid (:uid entry)]
                           (aset (.-style node) (final/camel-case rule) value)
                           (final/update-selection app-state)

                             ))  node-data))) )
                   :style-set-done
                   (fn [e]
                     (let [app-state (:app-state @data)
                           uids (:selection app-state)
                           node-data (vals (select-keys (:nodes app-state) uids))]

                       (dorun
                        (map
                         (fn [entry]
                           (let [node (:el entry)
                                 uid (:uid entry)]
                           (om/transact! data [:nodes uid :inline ] #(tokenize-style node)))) node-data)))) }})

(defn tool-lookup [view data]
  (let [comps {:mode mode :history history :outliner outliner :options options
               :style style :mini-map mini-map :file-browser file-browser}
        lenses {:mode data
                :history data
                :file-browser data
                :outliner data
                :style data
                :options data
                :mini-map (:dom data)
                :word-processor data}
        fns {:mode  [:mode :edit-settings :create-settings :wysiwyg :selection :nodes]
             :history  [:selection :mouse-target :element-filter :nodes]
             :file-browser [:mode :file-systems]
             :outliner [:dom :selection :mouse-target :element-filter :nodes :wysiwyg]
             :style [:style-select :style-use-pseudo :style-pseudo
                     :style-use-pseudo-element :style-pseudo-element
                     :style-target-breakpoint :mode :selection :nodes :wysiwyg]
             :options [:options]
             :mini-map []
             :word-processor []}]
    [(view comps) (view lenses) (view fns)]))




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
             str-view (apply str (rest (str view)))
             tabbed (:tabbed state)
             [x y w h] (or (:xywh state) [(:left state) (:top state) (:width state) (:height state)])
             style (if (:docked state)
                     #js {:height (px h)}
                     #js {:height (px h) :width (px w) :top (px y) :left (px x)})]

         (dom/div #js {:className "tool" :ref "tool" :style style}

            (sug/make draggable data {:opts {:className (str "title " (when tabbed "tabbed "))
                                            :content ""} ;(aget owner "_rootNodeID")) }

                                     :init-state {:message {:idx (:idx state)}
                                                  :drag-start :remember
                                                  :drag (if (:docked state) :drag-docked :drag-free)}})


           (dom/div #js {:className "title-overlay"}
               (sug/make drop-down data {:state {:active view} :opts {:onChange :set-view
                                                                    :options (:views (:app-state data))}})
               (dom/p nil str-view)

               (sug/make icon data {:state {:x -48 :y -64}})
               (sug/make icon data {:state {:x -32 :y -64}})
               (let [[cmx cmy] (if (:customize-tool state) [-16 -64] [-64 -48])]
               (sug/make icon data {:state {:x cmx :y cmy}
                                    :opts {:onClick #(om/update-state! owner :customize-tool not)}})))
            (when tabbed
              (apply dom/div #js {:className "tabs"}
                       (map (fn [tab] (dom/div #js {:className (str "label " (when (= view tab) "active "))
                                           :onClick #(om/set-state! owner :view tab) } (dom/p nil (str tab)))) tabbed)))

            (dom/div #js {:className "view"
                          :id (if (= view :outliner) "outliner_view" "")}
              (when-let [[component lense filter-keys] (tool-lookup view data)]
                (sug/make component lense {:init-state {:uid (:uid state) :custom {}}
                                           :react-key (:uid state)
                                           :state {:customize-tool (:customize-tool state)}
                                           :fn (fn [col] (update-in col [:app-state] #(select-keys % filter-keys)))})))

            (when-not (:docked state)
              (sug/make draggable data {:opts {:className "resize"}
                                     :init-state {:drag-start :remember
                                                  :drag :drag-resize}}))
             )))


   :on {:tool-resize (fn [e]) ;use this channel to broadcast resize to descendants
        :set-view
        (fn [e]
          (om/set-state! owner :view (:active e)))
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


