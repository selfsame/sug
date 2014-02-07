(ns examples.complex.data
  (:import [goog.ui IdGenerator]))

(def UID (atom 0))

(defn uid []
  (swap! UID inc))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))


(take 100 (repeatedly #(uid)))



(def INITIAL
  {:app {:mode {:active "edit"
                :options ["create" "edit"]}
         :style-select {:active "selection"
                :options ["selection" "css rule"]}
         :style-use-pseudo {:value true :text "pseudo selector:"}
         :style-pseudo {:active "hover"
                :options ["hover" "link" "visited" "active" "focus"]}
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
                     {:name :transform:rotate :icon "img/style_icons/rotate.png"}]

         :css-taxonomy {:measured #{:width :height
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
                                   :z-index}}}

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

               :left-shelf {:type :shelf
                 :align :left
                 :box {:left 0 :top 24 :width 300}
                 :spacing [.3 .7]
                 :stack [0 1]}

               :document {:type :document
                 :box {:top 24 :width 500 :height 700}}
               :right-shelf {:type :shelf
                 :align :right
                 :box {:right 0 :top 24 :width 300}
                 :spacing [.5 .15 .15 .2]
                 :stack [2 3 5 6]}

               :undocked {:stack [4]}
               :tool-boxes {0 {:type :mode
                             :docked :left
                             :box {:left 0 :top 0}}
                            1 {:type :style
                             :docked :left
                             :box {:left 0 :top 0}}
                            2 {:type :outliner
                             :docked :right
                             :box {:left 0 :top 0}}
                            3 {:type :history
                             :docked :right
                             :box {:left 0 :top 0}}
                            4 {:type :mode
                                     :docked :false
                                     :box {:height 250 :width 300
                                           :left 400 :top 120}}
                            5 {:type :mode
                             :docked :right
                             :box {:left 20 :top 0}}
                            6 {:type :style
                                     :docked :right
                                     :box {:left 0 :top 0}}
                            }
               }


   :file {}
   })


