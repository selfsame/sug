(ns examples.complex.data)

(def UID (atom 0))

(defn uid []
  (swap! UID inc))

(defn guid []
  (uid))


(defn rands [n] (take n (repeatedly
          #(keyword (str (uid))))))

(defn parts [V] (mapv #(apply conj %)  (partition-by #(= (rand-int 3) 1)
                                                             (shuffle V))))







(def KEYS-DOWN (atom #{}))

(def SELECTION-BOX (atom [[-100 -100][-99 -99]]))

(def MOUSE-DOWN-WORKSPACE (atom nil))
(def MOUSE-TARGET (atom nil))

(def OVER-HANDLE (atom nil))
(def MOUSE-DOWN (atom false))
(def MOUSE-DOWN-POS (atom nil))
(def MOUSE-POS (atom nil))

(def CSS-INFO {:measured #{:width :height
                                    :left :top :right :bottom
                                   :min-height :min-width
                                   :max-height :max-width
                                   :margin :padding :border
                                    :margin-top :margin-bottom
                                    :margin-left :margin-right
                                    :padding-top :padding-bottom
                                    :padding-left :padding-right
                                    :border-top :border-bottom
                                    :border-left :border-right
                                   :z-index :transform:rotate}

               :quad #{:margin :padding :border :border-radius}

               :compact #{:left :top :right :bottom
                          :min-height :min-width
                          :max-height :max-width
                          :z-index :color}

               :color-value #{:color :background-color}

               :css-rules [{:name :position :default "static"
                            :options ["" "static" "relative" "absolute" "fixed"]}
                           {:name :width :default "0px"
                            :icon "img/style_icons/width.png"
                            :sub-title "min/max"
                            :subs [{:name :min-width} {:name :max-width}]}

                           {:name :height :default "0px"
                            :icon "img/style_icons/width.png"
                            :sub-title "min/max"
                            :subs [{:name :min-height} {:name :max-height}]}
                           {:name :left :default "0px"}
                           {:name :top :default "auto"}
                           {:name :margin :default "0px"
                            :compound {:measure #{:! :1-4}}
                            :sub-title "individual"
                            :subs [{:name :margin-top :default "0px"}
                                   {:name :margin-left :default "0px"}
                                   {:name :margin-right :default "0px"}
                                   {:name :margin-bottom :default "0px"}]}
                           {:name :padding :default "0px"
                            :compound {:measure #{:! :1-4}}
                            :sub-title "individual"
                            :subs [{:name :padding-top} {:name :padding-left}
                                   {:name :padding-right} {:name :padding-bottom}]}

                           {:name :color :default "none"}
                           {:name :background-color :default "none"}

                           {:name :overflow :default "visible"
                            :options ["inherit" "visible" "hidden" "scroll"]
                            :icon "img/style_icons/overflow.png"}
                           {:name :box-sizing :default "content-box"
                            :options ["content-box" "border-box" "inherit"]}
                           {:name :clear :default "none"
                            :options ["none" "left" "right" "inherit"]
                            :icon "img/style_icons/clear.png"}
                           {:name :z-index :default "auto"}
                           {:name :transform:rotate :default ""
                            :icon "img/style_icons/rotate.png"}
                           {:name :border :default "0px"
                            :compound {:measure #{:?} :color #{:?} :word #{:?}}
                            :sub-title "advanced"
                            :subs [
;;                                    {:name :border-color :default "black"
;;                                     :compound {:color #{:?}}}
;;                                    {:name :border-style :default "solid"
;;                                     :options ["none" "solid" "dashed" "dotted"]
;;                                     :compound {:word #{:?}}}
                                   {:name :border-top :default "0px"}
                                   {:name :border-left :default "0px"}
                                   {:name :border-right :default "0px"}
                                   {:name :border-bottom :default "0px"}]}

                           ]})

(def HISTORY (atom []))

(def GLOBAL
  {})


(def INTERFACE
  {:wrapper
   {:app-state
    {:selection #{}
     :mouse-target -1
     :mode {:active "edit"
            :options [["create" "edit"]]}
     :wysiwyg false
     :edit-settings {:aspect {:active "offset"
                              :options [["offset" "margin"]]}
                     :show-margin {:value true :text "draw margins"}
                     :show-padding {:value true :text "draw padding"}}
     :create-settings {:create-type {:active "div"
                                     :options [["div" "p" "a" "span"]
                                               ["h1" "h2" "h3"]
                                               ["ul" "ol" "li"]
                                               ["form" "input" "button"]
                                               ["select" "option" "textarea"]
                                               ["header" "footer" "article" "menu"]]}
                       :create-position {:active "absolute"
                                         :options [["absolute"] ["relative"] ["fixed"]]} }

     :style-select {:active "selection"
                    :options [["selection" "css rule"]]}

     :style-target-breakpoint {:value false :text "breakpoint" :align :left}

     :style-use-pseudo {:value true :text "pseudo selector" :align :left}
     :style-pseudo {:active "hover"
                    :options [["hover" "link" "visited"] ["active" "focus"]]}
     :style-use-pseudo-element {:value true :text "pseudo selector" :align :left}
     :style-pseudo-element {:active "before"
                    :options [["before" "after"]]}

     :element-filter {:active "select-all"
                      :options [["p" "div" "all"]]}

     :node-filters {:expanded {:value true :text "expanded"}
                    :children {:value false :text "has children"}
                    :selected {:value false :text "selected"}
                    :styled {:value false :text "styled"}}
     :options {:rulers {:show {:value true :text "show rulers"}
                        :show-guides {:value true :text "show guides"}
                        :snap-guides {:value true :text "snap to guides"}}}
     :views {:mode [-32 0] :style [0 -16]
                       :outliner [-16 0] :history [0 -32] :options [0 0]}
               }

     }
   :commands {:toggle-app-mode {:key-down 9
                                :key-held []
                                :name "toggle edit/create mode"}
              :delete-selection {:key-down 88
                                :key-held [17]
                                :name "delete selection"}
              :duplicate-selection {:key-down 68
                                :key-held [17]
                                :name "delete selection"}

              :select-parent {:key-down 38
                                :key-held [17]
                                :name "select parent"}
              :select-children {:key-down 40
                                :key-held [17]
                                :name "select children"}
              :select-previous {:key-down 37
                                :key-held [17]
                                :name "select previous"}
              :select-next {:key-down 39
                                :key-held [17]
                                :name "select next"}}


   :interface {
               :menubar [{:key :file
                          :items [{:key :save-page
                                 :name "save page"
                                   :icon [-80 -96]}
                                {:key :save-page-as
                                 :name "save page as"
                                 :icon [-80 -64]}

                                {:key :browse-project
                                 :name "browse files"}
                                {:key :new-page
                                 :name "new page"
                                 :icon [-80 -48]}
                                {:key :new-project
                                 :name "new project"}]}
                         {:key :edit
                          :items [{:key :toggle-app-mode
                                 :name "toggle edit/create"}
                                {:key :undo
                                 :name "undo"
                                 :key-bind "ctrl-z"
                                 :icon [-64 -32]}
                                {:key :redo
                                 :name "redo"
                                 :key-bind "ctrl-alt-z"
                                 :icon [-80 -32]}]}
                         {:key :select
                          :items [{:key :select-parent
                                    :name "select parent"}
                                  {:key :select-children
                                    :name "select children"}]}
                         {:key :windows
                          :items [{:key :new-window
                                 :name "new window"}
                                   {:key :layouts
                                 :name "switch layout"}
                                   {:key :save-layout
                                 :name "save layout"}
                                 {:key :restore-default
                                 :name "restore default layout"}]}
                         {:key :options :items []}
                         {:key :script :items []}
                         {:key :breakpoints :items []}
                         {:key :fonts :items []}
                         {:key :help :items []}]

               :layout [.2 .65 .15]

               :_m {:window_w (.-innerWidth js/window)
                    :window_h (.-innerHeight js/window)
                    :scroll_y 0
                    :scroll_x 0
                    :outer_x 300
                    :outer_y 24
                    :outer_w 500
                    :outer_h 800
                    :iframe_w 0
                    :iframe_h 0
                    :ruler_w 16
                    :doc_scroll 0
                    :doc_w 0
                    :doc_h 0}

               :left-shelf {:align :left
                            :spacing [.3 .7]
                            :stack [
                                    {:uid (guid) :view :mode}
                                    {:uid (guid) :view :style}
                                    ]}


               :right-shelf {:align :right
                             :spacing [.6 .25 .15]
                             :stack [
                                     {:uid (guid) :view :outliner}
                                     ;{:uid (guid) :view :mini-map :tabbed [:mini-map :outliner]}
                                     {:uid (guid) :view :options }
                                     {:uid (guid) :view :history }]}

               :undocked [
                          ;{:uid (guid) :view :style :tabbed [:style :outliner :word-processor]
                          ; :xywh [700 100 280 400] }


                          ]}})



