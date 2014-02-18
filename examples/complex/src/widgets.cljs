(ns examples.complex.widgets
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [examples.complex.final :as final]
   [cljs.core.async :as async :refer [>! <! put! chan]])
  (:use
   [examples.complex.tokenize :only [tokenize-style]]
   [examples.complex.components :only [modal-box dom-node draggable bool-box]]
   [examples.complex.data :only [UID INITIAL CSS-INFO]]
   [examples.complex.util :only [value-from-node clear-nodes! location
                                 clog px to? from? within? get-xywh element-dimensions element-offset get-xywh]]))

(defn kstring [k]
  (apply str (rest (str k))))

(defn handle-change [e data owner]
  (prn "handle-change"))

(defn end-edit [e data owner]
  (let [state (om/get-state owner)
        node (om/get-node owner "input")
        value (int (.-value node))
        rstring (kstring (:name (:rule state)))]
    (sug/fire! owner :style-change {:rule rstring :value value})
    (sug/fire! owner :style-set-done {})))




(sug/defcomp style-widget
  [data owner opts]

   {:will-update
   (fn [_ _ next-state]
     (let []))
    :render-state
   (fn [_ state]
     (let [rule (:rule state)
           rule-name (kstring (:name rule))

           app (om/value data)
           nodes (:nodes app)
           selection (:selection app)
           filtered (conj {} (select-keys nodes selection ))
           inline-styles (or (apply merge (map :inline (vals filtered) )) {})

           inline ((:name rule) inline-styles)

           parsed (or (final/css-value inline) {})
           value (:value parsed)
           unit (:unit parsed)
           icon (:icon rule)
           measured ((:measured CSS-INFO) (:name rule))
           quad ((:quad CSS-INFO) (:name rule))
           compact ((:compact CSS-INFO) (:name rule))
           select-set (:options rule)
           sub-rules (:subs rule)
           root-classes (apply str "style-widget "

                               (cond inline "used ")

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

             (dom/input #js {:ref "input"
                             :value (or value "")
                             :onChange #(handle-change % data owner)
                             :onKeyPress #(when (== (.-keyCode %) 13)
                                            (end-edit % @data owner))
                             :onBlur (fn [e]
                                       ;(end-edit e @data owner)
                                       )} )))

         (when measured
           (dom/div #js {:className "unit"} (or unit "")))
         (when measured
           (sug/make draggable data {:opts {:className "scrub"}
                                     :init-state {:drag-start :scrub-start
                                                  :drag :scrub
                                                  :drag-stop :style-set-done}})))

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
                                                           :rule rule}})) sub-rules))))))
    :on {:scrub-start (fn [e]
                        (sug/private! owner :scrubbing true))

         :scrub (fn [e]
                  (let [dx (* (first (:diff-location e)) .5)
                        state (om/get-state owner)
                        node (om/get-node owner "input")
                        value (int (.-value node))
                        new-value (+ value dx)
                        rstring (kstring (:name (:rule state)))]
                    (aset node "value" new-value)
                    (sug/fire! owner :style-change {:rule rstring :value (+ value dx)}) ))}})



