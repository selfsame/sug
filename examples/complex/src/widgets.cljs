(ns examples.complex.widgets
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
   [cljs.core.async :as async :refer [>! <! put! chan]])
  (:use
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

(sug/defcomp style-widget [data owner opts]
  {:init-state
   (fn [_]
     (let [taxon (:taxonomy opts)
           rule (:name (om/value data))]
       {:name rule
        :measured ((:measured taxon) rule)
        :quad ((:quad taxon) rule)
        :compact ((:compact taxon) rule)
        :expanded false
        :select-set (:options (om/value data))}))
   :render-state
   (fn [_ state]
     (let [rule-name (apply str (rest (str (:name state))))
           icon (:icon (om/value data))
           sub-rules (:subs data)
           root-classes (apply str "style-widget "
                               (when icon "iconed ")
                               (when (:measured state) "measured ")
                               (when (:quad state) "quad ")
                               (when (:compact state) "compact "))]

     (dom/div #js {:className root-classes}
       (dom/div #js {:className "title"}
         (dom/div #js {:className "left"}
              (dom/p #js {:className "name"} rule-name ))
              (dom/div #js {:className "remove"} "."))
       (when icon
         (dom/img #js {:className "icon" :src icon}))
       (dom/div #js {:className "input-box"}
         (dom/div #js {:className "input-span"}
           (if (:select-set state)
             (apply dom/select nil
                (map #(dom/option #js {:value %} %) (:select-set state)))
             (dom/input #js {:value "none"})))

         (when (:measured state)
           (dom/div #js {:className "unit"}))
         (when (:measured state)
           (dom/div #js {:className "scrub"})))

        (when sub-rules
          (apply dom/div #js {:className (str (if (:expanded state)
                                                (str "subsection " "expanded ")
                                                "subsection ")
                                              (when (:quad state) "quad "))}
             (dom/p #js {:className "name"
                         :onClick #(om/set-state! owner :expanded (not (:expanded (om/get-state owner))))}
                    (:sub-title (om/value data)))
             (sug/make-all style-widget sub-rules {:opts opts})))

              )))})



