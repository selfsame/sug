(ns examples.complex.widgets
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
   [cljs.core.async :as async :refer [>! <! put! chan]])
  (:use
   [examples.complex.data :only [UID INITIAL CSS-INFO]]
   [examples.complex.util :only [value-from-node clear-nodes! location
                                 clog px to? from? within? get-xywh element-dimensions element-offset get-xywh]]))




(sug/defcomp style-delete [app owner opts]
  {:init-state
   (fn [_])
   :render-state
   (fn [_ state])})

(sug/defcomp style-scrub [app owner opts]
  {:init-state
   (fn [_])
   :render-state
   (fn [_ state])})

(sug/defcomp style-input [app owner opts]
  {:init-state
   (fn [_])
   :render-state
   (fn [_ state])})

(get {:a 5} :a)

(sug/defcomp style-widget
  [data owner opts]

   {:render-state
   (fn [_ state]
     (let [rule (:rule state)
           rule-name (apply str (rest (str (:name rule))))
           inline-styles (:styles state)
           inline ((:name rule) inline-styles)
           icon (:icon rule)
           measured ((:measured CSS-INFO) (:name rule))
           quad ((:quad CSS-INFO) (:name rule))
           compact ((:compact CSS-INFO) (:name rule))
           select-set (:options rule)
           sub-rules (:subs rule)
           root-classes (apply str "style-widget "
                               (when inline "used ")
                               (when icon "iconed ")
                               (when measured "measured ")
                               (when quad "quad ")
                               (when compact "compact "))]

     (dom/div #js {:className root-classes}
       (dom/div #js {:className "title"}
         (dom/div #js {:className "left"}
              (dom/p #js {:className "name"} (str rule-name (rand-int 100)) ))
              (dom/div #js {:className "remove"} "."))
       (when icon
         (dom/img #js {:className "icon" :src icon}))
       (dom/div #js {:className "input-box"}
         (dom/div #js {:className "input-span"}
           (if select-set
             (apply dom/select nil
                (map #(dom/option #js {:value %} %) select-set))
             (dom/input #js {:value (or inline "none")})))

         (when measured
           (dom/div #js {:className "unit"}))
         (when measured
           (dom/div #js {:className "scrub"})))

        (when sub-rules
          (apply dom/div #js {:className (str (if (:expanded state)
                                                (str "subsection " "expanded ")
                                                "subsection ")
                                              (when quad "quad "))}
             (dom/p #js {:className "name"
                         :onClick #(om/set-state! owner :expanded (not (:expanded (om/get-state owner))))}
                    (:sub-title rule))
             (map (fn [rule]
              (sug/make style-widget data {:state {:styles (:style state)
                                                           :rule rule}})) sub-rules))))))})



