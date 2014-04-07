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
                                   :z-index}
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
                           {:name :left :default "auto"}
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

(def GLOBAL
  {})


(def INTERFACE
  {:wrapper {:app-state {:selection #{}
               :mouse-target -1
               :mode {:active "edit"
                      :options ["create" "edit"]}
               :style-select {:active "selection"
                              :options ["selection" "css rule"]}
               :style-use-pseudo {:value true :text "pseudo selector:"}
               :style-pseudo {:active "hover"
                              :options ["hover" "link" "visited" "active" "focus"]}
               :element-filter {:active "select-all"
                      :options ["p" "div" "all"]}

               :node-filters {:expanded {:value true :text "expanded"}
                              :children {:value false :text "has children"}
                              :selected {:value false :text "selected"}
                              :styled {:value false :text "styled"}}

                         }}

   :interface {:menubar {:file [{:key :open-project
                                 :name "browse files"}
                                {:key :new-project
                                 :name "new project"}]
                         :edit [{:key :undo
                                 :name "undo"
                                 :key-bind "ctrl-z"}
                                {:key :redo
                                 :name "redo"
                                 :key-bind "ctrl-alt-z"}]
                         :insert []
                         :options []
                         :script []
                         :breakpoints []
                         :fonts []
                         :help []}

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
                                    {:uid (guid) :view :style :tabbed [:style :history]}
                                    ]}


               :right-shelf {:align :right
                             :spacing [.6 .25 .15]
                             :stack [{:uid (guid) :view :outliner}
                                     {:uid (guid) :view :mini-map :tabbed [:mini-map :outliner]}
                                     {:uid (guid) :view :mode }]}

               :undocked [
                          {:uid (guid) :view :style :tabbed [:style :outliner :word-processor]
                           :xywh [700 100 280 400] }


                          ]}})



