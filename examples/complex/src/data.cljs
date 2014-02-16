(ns examples.complex.data
  (:import [goog.ui IdGenerator]))

(def UID (atom 0))

(defn uid []
  (swap! UID inc))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))


(defn rands [n] (take n (repeatedly
          #(keyword (str (uid))))))

(defn parts [V] (mapv #(apply conj %)  (partition-by #(= (rand-int 3) 1)
                                                             (shuffle V))))






(defn ITER [V]
  (let [P (parts V)
        C (count P)
      S (rands C)]
  (map (fn [s p] {s p}) S P)))

(ITER (map (fn [n] {n {}}) (rands 100)) )

;(def MESS (ITER (ITER (ITER (ITER (ITER (ITER (map (fn [n] {n {}}) (rands 1000)) )))))))





(def MOUSE-TARGET (atom nil))

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
                                   :z-index}
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
                           {:name :transform:rotate :icon "img/style_icons/rotate.png"}]})

(def GLOBAL
  {})


(def INTERFACE
  {:app-state {:selection #{}
               :filtered {:inline {} :nodes {}}
               :mouse-target -1
               :mode {:active "edit"
                      :options ["create" "edit"]}
               :style-select {:active "selection"
                              :options ["selection" "css rule"]}
               :style-use-pseudo {:value true :text "pseudo selector:"}
               :style-pseudo {:active "hover"
                              :options ["hover" "link" "visited" "active" "focus"]}
               :element-filter {:active "select-all"
                      :options ["p" "div" "all"]}}

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
                    :scroll_x 0
                    :scroll_y 0
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
                            :stack [{:uid (guid) :view :mode}
                                    {:uid (guid) :view :style :tabbed [:style :history]}]}


               :right-shelf {:align :right
                             :spacing [.6 .15 .15]
                             :stack [{:uid (guid) :view :outliner}
                                     {:uid (guid) :view :history}
                                    {:uid (guid) :view :mode }]}

               :undocked [
                          {:uid (guid) :view :history
                           :xywh [400 200 320 500] }

                          ]}})

