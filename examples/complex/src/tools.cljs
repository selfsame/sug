(ns examples.complex.tools
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.reader :as reader]
      [domina]
   [examples.complex.widgets :as widgets]
   [cljs.core.async :as async :refer [>! <! put! chan]]
      )
  (:use
    [examples.complex.data :only [UID INITIAL]]
   [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within? get-xywh element-dimensions element-offset get-xywh]]
    [examples.complex.components :only [modal-box dom-node draggable bool-box]]))




(defn mode [app owner opts]
  (reify
    om/IRender
    (render [_]
       (let [el-fil (:mode (:app app))]
            (om/build modal-box el-fil) ))))

(defn history [app owner opts]
  (reify
    om/IRender
    (render [_]
       (let []
            (dom/p nil "unimplemented") ))))

(defn outliner [app owner opts]
  (reify
    om/IRender
    (render [_]
      (let [el-fil (str "select-" (:active (:element-filter (:app app))))]
        (dom/div #js {:className el-fil
                      :id "outliner"}
          (into-array
             (om/build-all dom-node (:dom app))))  ))))


(sug/defcomp style [app owner opts]
  {:render-state
   (fn [_ state]
       (let [select (:style-select (:app app))
             pseudo (:style-use-pseudo (:app app))
             pseudo-opts (:style-pseudo (:app app))
             css-rules (:css-rules (:app app))
             taxonomy (:css-taxonomy (:app app))]
         (om/set-state! owner :use-psuedo (:value pseudo))
         (apply dom/div #js {:className (str "options" (when (= "create" (:active (:mode (:app app)))) " disabled"))}
            (om/build modal-box select)
            (om/build bool-box pseudo)
            (om/build modal-box pseudo-opts {:opts {:disabled (not (:value pseudo))}} )
            (sug/make-all widgets/style-widget css-rules {:opts {:taxonomy taxonomy}}))
                  ))})

(def tool-lookup {:mode mode :outliner outliner :history history :style style})

(defn docked? [k]
  (if (or (= :left k) (= :right k)) true false))

(defn vec-wrap
  ([arg]
    (if (sequential? arg)
      (vec arg)
      (if-not (nil? arg)
        (vector arg)
        [] )))
  ([arg & more]
   (let [args (cons arg more)]
    (if (sequential? args)
      (vec args)
      (vector args)))))





(sug/defcomp toolbox [app owner opts]
  {:init-state
   (fn [_]
      (let [uid (:uid opts)
            tool-data (get (:tool-boxes (:interface (om/value app) )) uid)
            docked (:docked tool-data)
            box (conj (:box tool-data) {})]
        (if (docked? docked)
          {:box (conj (:box tool-data) {:height (:height opts)}) :docked docked}
          {:box box :docked docked} )))

    :will-update
    (fn [_ next-props next-state]
       (let [state (om/get-state owner)
             uid (:uid state)
             box (:box next-state)
             tool-data (get (:tool-boxes (:interface (om/value app) )) uid)
             docked (:docked (om/get-state owner))
             node   (om/get-node owner "tool")
             [x y w h]  (get-xywh node)] ))

    :render-state
    (fn [_ state]
       (let [uid (:uid state)
             tool-data (get (:tool-boxes (:interface (om/value app) )) uid)
             tool-type (:type tool-data)
             box (:box state)
             width (:width box)
             left (if (number? (:left box)) (:left box) 0)
             top (if (number? (:top box)) (:top box) 0)
             docked (:docked (om/get-state owner))
             height (if (docked? docked) (:height state) (:height box))
             style (cond (docked? docked) #js {:height (px height) :top (px top) }  ;(px (:height opts)) }
                     :else #js {:height (px height) :width (px width) :top (px top) :left (px left)})]

         (dom/div #js {:className "tool" :ref "tool" :style style}
            (sug/make draggable app {:opts {:className "title"
                                            :content (str tool-type ": " uid )}
                                     :state {:uid uid}
                                     :init-state {:uid uid :fire-listener (:fire-listener tool-data)}})

            (dom/div #js {:className "view"
                          :id (if (= tool-type :outliner) "outliner_view" "")}
              (when (tool-lookup tool-type)
                (om/build (tool-lookup tool-type) app))))))

   :on {:drag (fn [e]
                (if (docked? (om/get-state owner :docked))
                  (let [el (om/get-node owner "tool")
                        xywh (get-xywh el)]
                    (sug/fire! owner :docked-drag (conj e {:box (zipmap [:left :top :width :height] xywh)})))
                  ;else
                  (do (let [state (om/get-state owner)
                            box (:box state)
                            xy [(:left box) (:top box)]
                            newpos (map + xy (:diff-location e))]
                        (om/set-state! owner :box (conj box {:left (first newpos) :top (last newpos)}))))))}})


