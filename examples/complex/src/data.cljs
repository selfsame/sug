(ns examples.complex.data
  (:import [goog.ui IdGenerator]))

(def UID (atom 0))

(defn uid []
  (swap! UID inc))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))


(def CSS-INFO {:measured #{:width :height
                                    :left :top :right :bottom
                                   :min-height :min-width
                                   :max-height :max-width
                                    :margin-top :margin-bottom
                                    :margin-left :margin-right
                                    :padding-top :padding-bottom
                                    :padding-left :padding-right
                                   :z-index :transform:rotate}

                        :quad #{:margin :padding :border :border-radius}

                        :compact #{:left :top :right :bottom
                                   :min-height :min-width
                                   :max-height :max-width
                                   :z-index}})

(def GLOBAL
  {})


(def INTERFACE
  {:app-state {:selection #{}
               :mouse-target -1
               :mode {:active "create"
                      :options ["create" "edit"]}
               :style-select {:active "selection"
                              :options ["selection" "css rule"]}
               :style-use-pseudo {:value true :text "pseudo selector:"}
               :style-pseudo {:active "hover"
                              :options ["hover" "link" "visited" "active" "focus"]}
               :element-filter {:active "select-all"
                      :options ["p" "div" "all"]}
               :css-rules [{:name :width :icon "img/style_icons/width.png"
                            :sub-title "min/max"
                            :subs [{:name :min-width} {:name :max-width}]}

                           {:name :height :icon "img/style_icons/width.png"
                            :sub-title "min/max"
                            :subs [{:name :min-height} {:name :max-height}]}
                           {:name :left}
                           {:name :top}
                           {:name :margin
                            :sub-title "individual"
                            :subs [{:name :margin-top} {:name :margin-left}
                                   {:name :margin-right} {:name :margin-bottom}]}
                           {:name :padding
                            :sub-title "individual"
                            :subs [{:name :padding-top} {:name :padding-left}
                                   {:name :padding-right} {:name :padding-bottom}]}
                           {:name :overflow :options ["inherit" "visible" "hidden" "scroll"]
                            :icon "img/style_icons/overflow.png"}
                           {:name :box-sizing :options ["content-box" "border-box" "inherit"]}
                           {:name :clear :options ["none" "left" "right" "inherit"]
                            :icon "img/style_icons/clear.png"}
                           {:name :z-index}
                           {:name :transform:rotate :icon "img/style_icons/rotate.png"}]}

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

               :layout [.1 .8 .1]

               :left-shelf {:align :left
                            :spacing [.2 .5 .3]
                            :stack [{:view :mode}
                                    {:view :style :tabbed [:style :history]}
                                    {:view :mode :tabbed [:mode :style :history :outliner]}]}

               :document {}

               :right-shelf {:align :right
                             :spacing [.5 .15 .15]
                             :stack [{:view :outliner}
                                    {:view :history}
                                    {:view :mode}]}

               :undocked {:stack [{:view :outliner :xywh [100 100 220 400] }]}}})

