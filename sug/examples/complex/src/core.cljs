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
   [examples.complex.filebrowser :as filebrowser]
   [goog.events :as events])
  (:use

    [examples.complex.data :only [UID GLOBAL INTERFACE MOUSE-TARGET KEYS-DOWN
                                  OVER-HANDLE MOUSE-DOWN-POS MOUSE-DOWN MOUSE-POS MOUSE-DOWN-WORKSPACE INSERTION]]
    [examples.complex.util :only [expanded-node value-from-node clear-nodes! location clog px to? from? within? style!
                     descendant? get-xywh element-dimensions element-offset exclude toggle]]
    [examples.complex.components :only [icon modal-box dom-node draggable]])
  (:import
   [goog.events EventType]))

(enable-console-print!)

(declare DATA)



(defn _t []
  (aget js/window "_t"))

(defn position-native [left top width height]
  (aset (.-_m js/window) "outer_x" left)
  (aset (.-_m js/window) "outer_y" top)
  (aset (.-_m js/window) "outer_w" width)
  (aset (.-_m js/window) "outer_h" height)

  (for [id ["workspace"]]
    (let [rule (if (= "iframe_proxy" id) 16 0)]
    (do
    (aset (.-style (.getElementById js/document id)) "left" (px (+ left rule) ))
      (aset (.-style (.getElementById js/document id)) "width" (px (- width rule) ))
      (aset (.-style (.getElementById js/document id)) "height" (px (- height rule) ))
      (aset (.-style (.getElementById js/document id)) "top" (px (+ top rule)) )))))



(defn idx-for [v coll]
  (keep-indexed #(if (= %2 v) %1 nil) coll))

(defn changed?
  ([state1 state2]
   (changed? state1 state2 []))
  ([state1 state2 korks]
   (let [ks (if (sequential? korks) korks [korks])]
     (not= (get-in state1 ks)
           (get-in state2 ks)))))


(defn if-state-change [owner korks yes no]
  (let [state (om/get-state owner korks)
        render-state (om/get-render-state owner korks)]
  (if (not= state render-state) yes no)))



(sug/defcomp dialogue
  [data owner opts]
  {:render-state
    (fn [_ state]
      (let [k (om/value (:key data))]
      (dom/div #js {:className "menu_option"
                    :onClick #(commands/route-menu k)}
         (dom/span nil (:name data))
         (dom/span #js {:className "binding"} (:key-bind data)))))})




(sug/defcomp menu-option
  [data owner opts]
  {:render
    (fn [_]
      (let [k (om/value (:key data))]
      (dom/div #js {:className "menu_option"
                    :onClick #(commands/route-menu k)}
         (when (:icon data)
           (sug/make icon data {:state {:x (first (:icon data)) :y (last (:icon data))}}))
         (dom/span nil (:name data))
         (dom/span #js {:className "binding"} (:key-bind data)))))})



(sug/defcomp menubar
  [data owner opts]

  {:render
    (fn [_]
      (dom/div #js {:className "menubar"}
           (into-array
            (map (fn [menu]
                   (dom/div #js {:className "menu"}
                      (dom/p nil (apply str (rest (str (:key menu)))))
                        (apply dom/div #js {:className "menu_box"}
                            (sug/make-all menu-option (:items menu) {} ))))
                 data) )))})


(sug/defcomp tool-shelf
  [data owner opts]

  {:render-state
  (fn [_ state]
      (let [shelf (:shelf state)
            stack (:stack shelf)
            align (:align shelf)
            style (if (= :right align)
                    #js {:top (:top state) :right (px (:right state)) :width (px  (:width state))}
                    #js {:top (:top state) :left (px (:left state)) :width (px (:width state))})]

        (dom/div #js {:className "shelf" :ref "shelf" :style style}
               (apply dom/div nil
                 (for [idx (range 0 (count stack))
                        :let [tool (nth stack idx)
                              tool-height (nth (:spacing state) idx)]]

                           (sug/make tools/toolbox data {:react-key (:uid tool)
                                                         :init-state {:idx idx
                                                                      :uid (:uid tool)
                                                                      :view (:view tool)
                                                                       :tabbed (:tabbed tool)
                                                                       :docked true
                                                                      :align (:align state)
                                                                      :height tool-height}})))


                  (let [handle-style (if (= :right align)
                    #js {:right (px (:width state)) :top (px (:top state))}
                    #js {:left (px (:width state)) :top (px (:top state))})]
                 (sug/make draggable (:app-state data) {:opts {:className "handle"
                                                   :style handle-style}
                                            :init-state {:drag (keyword (str "drag-shelf" align))}
                                            :state {:width (:width state)}})))))

   :on {:drag-docked
        (fn [e] (sug/fire! owner :alter-shelf-spacing (conj e {:align (:align (om/get-state owner))})) )}})

(defn lay-out [data owner changed]
  (let [changes (if (set? changed) changed #{})]
  (when (or (changes :window)
            (changes :layout))
    (let [state (om/get-state owner)
          _m (:_m (:interface data))
          [ww wh] [(:window_w _m) (:window_h _m)]
          layout (:layout state)
          old-w (apply + layout)
          lay-perc (mapv #(/ % old-w) layout)
          new-layout (mapv * lay-perc [ww ww ww])
          [ll lm lr] new-layout
          outer_x (+ ll 8 )
          outer_w (- lm 16)
          outer_h (- wh 16)]

        {:layout new-layout
         :outer_x outer_x
         :outer_w outer_w
         :outer_h outer_h} ))))


(sug/defcomp interface
  [data owner opts]
  {:init-state
   (fn [_]
     (let [ww (:window_w (:_m (:interface data)))
            wh (:window_h (:_m (:interface data)))
            shelf-h (- wh 24)
            layout (:layout (:interface data))
            left-shelf (:left-shelf (:interface data))
            right-shelf (:right-shelf (:interface data))
            left-spacing (mapv #(* shelf-h %) (:spacing left-shelf))
            right-spacing (mapv #(* shelf-h %) (:spacing right-shelf))
            windows (into {}
                     (map (fn [w]
                          {(:uid w) w})
                        (:undocked (:interface data))))]

       {:layout (mapv * layout [ww ww ww])
        :windows windows
        :left-spacing left-spacing
        :right-spacing right-spacing
        :left-shelf (:left-shelf (:interface data))
        :right-shelf (:right-shelf (:interface data))} ))
  :will-mount
   (fn [_] )
  :will-update
   (fn [_ next-props next-state]
     (let [state (om/get-render-state owner)
           layout (:layout state)
           _m (:_m (:interface data))
           next_m (:_m (:interface next-props))
           changed (set (filter keyword? (conj []
                     (when (changed? data next-props [:wrapper :app-state :selection]) :selection)
                     (when (changed? state (sug/private owner) :layout) :layout)
                     (when (or (changed? _m next_m :window_w)
                               (changed? _m next_m :window_h)) :window)
                     (when (or (changed? _m next_m :scroll_x)
                               (changed? _m next_m :scroll_y)) :scroll) )))]

       (sug/private! owner :changes changed)
       (sug/private! owner :layout layout)
       (sug/private! owner :left-spacing (:left-spacing state))
       (sug/private! owner :right-spacing (:right-spacing state))

       (when (changed? data next-props [:wrapper :app-state :selection])

         (sug/private! owner :new-selection true))))
  :did-mount
   (fn [_]
     (om/transact! data [:interface :_m]
                       (fn [v] (conj v {:window_w (.-innerWidth js/window)
                                        :window_h (.-innerHeight js/window)})))
     (final/draw-rulers))
  :render-state
    (fn [_ state]
      (let [render-state (om/get-render-state owner)
            private (sug/private owner)
            changes (sug/private owner :changes)
            new_m (lay-out data owner changes)
            ww (:window_w (:_m (:interface data)))
            wh (:window_h (:_m (:interface data)))
            layout (:layout state)]

      (when new_m
         (dorun (position-native (:outer_x new_m) 24 (:outer_w new_m) (:outer_h new_m))))


       (when (or new_m
                ((set changes) :scroll)
                (sug/private owner [:new-selection]))
         (do
           (final/update-selection (:app-state (:wrapper data)))
           (sug/private! owner :new-selection false)))


        (dom/div nil
                 (om/build menubar (:menubar (:interface data)) {})
                 (sug/make tool-shelf (:wrapper data) {:init-state {:align :left :shelf (:left-shelf (:interface data))
                                                               :width (first layout) :height (- wh 24) :left 0 :top 24}
                                                 :state (conj {}
                                                              (when (changed? private state [:layout 0])
                                                                {:width (first layout)})
                                                              (when (changed? private state :left-spacing)
                                                                {:spacing (:left-spacing state)}
                                                                )
                                                          )})
                 (sug/make tool-shelf (:wrapper data) {:init-state {:align :right :shelf (:right-shelf (:interface data))
                                                         :width (last layout) :height (- wh 24) :right 0 :top 24}
                                            :state (conj {}
                                                              (when (changed? private state [:layout 2])
                                                                {:width (last layout)})
                                                              (when (changed? private state :right-spacing)
                                                                {:spacing (:right-spacing state)})
                                                          )})


                 (apply dom/div #js {:className "undocked"}
                        (map (fn [w]
                               (sug/make
                                tools/toolbox
                                (:wrapper data)
                                {:react-key (:uid w)
                                 :init-state {:uid (:uid w)
                                              :view (:view w)
                                              :xywh (:xywh w)
                                              :docked false
                                              :tabbed (:tabbed w)}}))
                             (vals (:windows state)))))))

   :on {
        :drag-shelf:left
        (fn [e]
          (let [layout (om/get-state owner :layout)
                diffpos (:diff-location e)
                xd (first diffpos)]
            (om/set-state! owner :layout (mapv + layout [xd (- xd) 0]))))

        :drag-shelf:right
        (fn [e]
          (let [layout (om/get-state owner :layout)
                diffpos (:diff-location e)
                xd (first diffpos)]
            (om/set-state! owner :layout (mapv + layout [0 xd (- xd)]))))

        :resize-docked (fn [e] )
        :alter-shelf-spacing
        (fn [e]
          (let [state (om/get-state owner)
                [spacing-align shelf-align] (cond (= :left (:align e)) [:left-spacing :left-shelf]
                                                  (= :right (:align e)) [:right-spacing :right-shelf])
                spacing (spacing-align state)
                idx (:idx e)
                stack (:stack @(shelf-align state))
                get-uid (fn [i stk] (:uid (nth stk i)))
                shelf-uid (get-uid idx stack)
                prev-uid (if-not (neg? (dec idx)) (get-uid (dec idx) stack) nil)
                [dx dy] (:diff-location e)]

            (sug/fire-down! owner [:resize-docked] {:uid shelf-uid :dh (- dy) :align (:align e)})
            (when prev-uid
               (sug/fire-down! owner [:resize-docked] {:uid prev-uid :dh dy :align (:align e)}))

            ))
        }})



(defn update-scroll [e]
  (let [target (.-target e)
        [sx sy] [(.-scrollLeft target) (.-scrollTop target)]]
    (swap! DATA update-in [:interface :_m] #(conj % {:scroll_x sx :scroll_y sy}))
    (aset (.-_m js/window) "scroll_x" sx)
    (aset (.-_m js/window) "scroll_y" sy)))

(defn doc->workspace [xy]
  (let [ox (aget (.-_m js/window) "outer_x")
        oy (aget (.-_m js/window) "outer_y")]
    (mapv - xy [ox oy] [16 16])))



(defn check-mouseup [e]
  (let [target (.-target e)
        uid (.-uid target)
        nodes (get-in @DATA [:wrapper :app-state :nodes])

        expanded-uid (expanded-node nodes uid)
        [x y] (doc->workspace [(.-clientX e) (.-clientY e)])]

    (swap! MOUSE-DOWN #(identity false))
    (swap! MOUSE-DOWN-WORKSPACE #(identity false))
    (swap! MOUSE-POS #(identity [x y]))
    (let [[dx dy] (mapv - @MOUSE-POS @MOUSE-DOWN-POS)]
        (if (and (< -2 dx 2)(< -2 dy 2))
           (when uid
             (swap! DATA update-in [:wrapper :app-state :selection] #(tools/select! % expanded-uid)))
           (when @OVER-HANDLE
             (final/finalize-handle-interaction! DATA))))))

(defn check-mousedown [e data owner]
  (let [target (.-target e)
        [x y] (doc->workspace [(.-clientX e) (.-clientY e)])
        app-mode (get-in @DATA [:wrapper :app-state :mode :active])]

    (when (descendant? target (js/workspace "html"))
      (swap! MOUSE-DOWN-WORKSPACE #(identity true))
      (when (= "create" app-mode) (final/create-element e [x y] DATA INSERTION data owner)))
    (swap! MOUSE-DOWN #(identity true))
    (swap! MOUSE-DOWN-POS #(identity [x y]))
    (swap! MOUSE-POS #(identity [x y]))))


(defn show-insertion [e x y]
  (let [target (.-target e)
        on-workspace (descendant? target (js/workspace "html"))]
    (when on-workspace
      (let [insertion (.find_insertion (.-arrange js/_t) (clj->js {:target (.-target e) :clientX x :clientY y}))]
        (swap! INSERTION #(identity insertion))
        (final/set-cursor :workspace "crosshair")
        (.display (.-arrange js/_t) insertion)
        (.draw_arrange (.-tracking js/_t))))))

(defn check-mousemove [e]
  (let [target (.-target e)
        uid (.-uid target)
        uid-path (.-uid_path target)
        nodes (get-in @DATA [:wrapper :app-state :nodes])
        app-mode (get-in @DATA [:wrapper :app-state :mode :active])
        expanded-uid (expanded-node nodes uid)
        [x y] (doc->workspace [(.-clientX e) (.-clientY e)])]
    (when (= "create" app-mode) (show-insertion e x y))

    (swap! MOUSE-POS #(identity [x y]))
    (if (not @MOUSE-DOWN)
        (final/check-over-resize e [x y]))
    (if @MOUSE-DOWN-WORKSPACE
        (if @OVER-HANDLE
          (final/handle-interaction! DATA))
      (when-not (and @MOUSE-DOWN (= @MOUSE-TARGET expanded-uid))
            (swap! MOUSE-TARGET #(identity expanded-uid))
            ;(swap! DATA update-in [:wrapper :app-state :mouse-target] #(identity uid))
            (final/update-selection (get-in @DATA [:wrapper :app-state]))

        ))))

(defn set-contains-col [s col]
  (not-any? nil? (map s col)))

(defn route-key-commands [code data owner]
  (let [commands (:commands @data)
        keys-down @KEYS-DOWN
        result (filter
                (fn [[k v]]
                  (and
                   (= code (:key-down v))
                   (set-contains-col keys-down (:key-held v)))) commands)]

    (when (first result)
      (let [command (first result)
            ck (first command)
            cv (last command)]

      (prn :COMMAND (first result) ck)
      (sug/emit! ck {:down keys-down} )))))

(defn handle-keydown [e data owner]
  (let [target (.-target e)
        code (.-keyCode e)]
    (prn code)
    (route-key-commands code data owner)
    (swap! KEYS-DOWN conj code)))

(defn handle-keyup [e data owner]
  (let [target (.-target e)
        code (.-keyCode e)]
    (swap! KEYS-DOWN disj code)))



(defn filter-data [data]
  "filters data depending on the current selection"
  (let [app-state (:app-state (:wrapper data))
        app (om/value app-state)
        nodes (:nodes app)
        selection (:selection app)
        filtered (conj {} (select-keys nodes selection ))
        inline (or (apply merge (map :inline (vals filtered) )) {})]
    (update-in data [:wrapper :app-state :filtered]
          (fn [x] {:inline inline
                   :selected-nodes (or (vals filtered) [])}))))


 (defn alter-selection [e data alter-fn]
   (let [app-state (:app-state (:wrapper @data))
         selection (:selection app-state)
         nodes (:nodes app-state)
         s-nodes (select-keys nodes selection)
         altered-uids (flatten (map alter-fn (vals s-nodes)))
         shift (contains? (:down e) 16)]
     ;(prn (vals s-nodes))
     (om/transact! data [:wrapper :app-state :selection] (if shift
                                                           #(apply conj % altered-uids)
                                                           #(set altered-uids))) ))


(sug/defcomp dom-app
  [data owner opts]

  {:will-mount
   (fn [_]
     (.mousedown (js/$ js/window) #(check-mousedown % data owner))
      (.keydown (js/$ js/window)  #(handle-keydown % data owner))
        (.keyup (js/$ js/window)  #(handle-keyup %  data owner)))
   :will-update
   (fn [_ next-props next-state]
     (let [state (om/get-render-state owner)]))
   :did-mount
   (fn [_]
     (doto js/window
       (events/listen
        EventType.RESIZE
        #(om/transact! data [:interface :_m]
                       (fn [v] (conj v {:window_w (.-innerWidth js/window)
                                        :window_h (.-innerHeight js/window)}))))))
   :render
    (fn [_]
        (sug/make interface data {})
      )
   :catch {:dom-restructure
        (fn [e]

          (let [{:keys [dom nodes]} (tokenize/remake data)]
            (om/transact! data [:wrapper :app-state] #(conj % {:nodes nodes :dom dom})) ))
        :save-state (fn [e] )
        :toggle-app-mode
        (fn [e] (om/transact! data [:wrapper :app-state :mode :active]
                              #(if (= % "edit") "create" "edit")))
        :select-children
        (fn [e] (alter-selection e data :children))
        :select-parent
        (fn [e] (alter-selection e data :parent))
        :delete-selection
        (fn [e]
          (let [app-state (:app-state (:wrapper @data))
                selection (:selection app-state)
                nodes (:nodes app-state)
                dom (:dom app-state)
                elements (map :el (vals (select-keys nodes selection)))
                parent-uids (map :parent (vals (select-keys nodes selection)))

                ]

            ;(prn selection)
            ;(prn parent-uids)

            ;(dorun (map #(tokenize/untokenize % nodes dom) selection))
;;             (om/transact! data [:wrapper :app-state]
;;                           #(conj % (tokenize/untokenize (first selection) nodes dom) ))
;;             (dorun (map #(.remove %) elements))
;;             (om/transact! data [:wrapper :app-state :selection] #(identity #{}))
            ))
        }})


(defn watch-nodes [m t]
  ;(prn (:path m) (:old-value m) (:new-value m))
  )

(js/$ (fn []
  (def NODES (atom {}))
  (def DATA (atom (update-in INTERFACE [:wrapper :app-state] #(conj %
                                                                  {:dom [(tokenize/make (first (.toArray (js/$$ "body"))) [] NODES)]
                                                                   :nodes @NODES}))))

  (swap! DATA update-in [:wrapper :app-state :file-systems :data :local-storage] filebrowser/get-local-files)


  (om/root dom-app DATA {:target (.getElementById js/document "main")
                         :tx-listen (fn [m t] (watch-nodes m t)
                                      )})




        (.bind (js/$ js/window) "mousemove" check-mousemove)
        (.on (js/$ js/window) "mouseup" #(check-mouseup %))


        (doto (.getElementById js/document "doc-scroll")
          (events/listen EventType.SCROLL #(update-scroll %)))

  (aset js/window "_m" (clj->js  (:_m (:interface @DATA))))
  (.init (.-tracking (_t)))

  (let [i-proxy (.getElementById js/document "iframe_proxy")
        [iw ih] (final/calc-iframe-dim)]
    (style! i-proxy :height (px ih)))))


