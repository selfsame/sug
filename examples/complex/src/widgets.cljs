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
   [examples.complex.data :only [UID CSS-INFO]]
   [examples.complex.util :only [value-from-node clear-nodes! location multiple?
                                 clog px to? from? within? get-xywh element-dimensions element-offset get-xywh]]))

(defn kstring [k]
  (apply str (rest (str k))))

(defn handle-change [e data owner]
  (prn "handle-change"))

(defn end-edit [e data owner]
  (let [state (om/get-state owner)
        node (om/get-node owner "input")

        rule (:rule state)
        measured ((:measured CSS-INFO) (:name rule))
        value (if measured (px (int (.-value node))) (.-value node))
        rstring (kstring (:name rule))]
    (sug/fire! owner :style-change {:rule rstring :value value})
    (sug/fire! owner :style-set-done {})))


(defn measures->string [col]
  (apply str (interpose " " (map #(str (:value %) (:unit %)) col))))

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

           quad ((:quad CSS-INFO) (:name rule))
           inline-styles (or (apply merge (map :inline (vals filtered) )) {})

           inline ((:name rule) inline-styles)
           computed (or inline  (.css (js/$ (:el (first (vals filtered)))) rule-name ))

            parsed (if (nil? computed) [] (final/css-values computed))
            compound (and (:compound rule)
                          (< 1 (count parsed)))
           value (if compound
                   (measures->string parsed)
                   (:value (first parsed)))
           unit (:unit (first parsed))


           icon (:icon rule)
           measured ((:measured CSS-INFO) (:name rule))

           compact ((:compact CSS-INFO) (:name rule))
           select-set (:options rule)
           sub-rules (:subs rule)
           root-classes (apply str "style-widget "

                               (if inline
                                 "used "
                                 (if (not= computed (:default rule)) "computed "))

                               (when icon "iconed ")
                               (when measured "measured ")
                               (when quad "quad ")
                               (when compact "compact "))]


     (dom/div #js {:className root-classes}
       (dom/div #js {:className "title"}
         (dom/div #js {:className "left"}
              (dom/p #js {:className "name"} (str rule-name (comment (rand-int 100)) ) ))
              (dom/div #js {:className "remove"
                            :onClick #(do
                                        (sug/fire! owner :style-change {:rule rule-name :value ""})
                                        (sug/fire! owner :style-set-done {})) } "."))
       (when icon
         (dom/img #js {:className "icon" :src icon}))
       (dom/div #js {:className "input-box"}
         (dom/div #js {:className "input-span"}
           (if select-set
             (apply dom/select #js {:ref "input"
                                    :value (or value "")
                                    :onChange #(end-edit % data owner)}
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
                                     :init-state {:message {:name (:name rule)}
                                                  :drag-start :scrub-start
                                                  :drag :scrub
                                                  :drag-stop :scrub-stop}})))

        (when sub-rules
          (apply dom/div #js {:className (str "clearfix subsection "
                                              (when (:expanded state)
                                                "expanded ")
                                              (when quad "quad "))}
             (dom/p #js {:className "name"
                         :onClick #(om/set-state! owner :expanded (not (:expanded (om/get-state owner))))}
                    (:sub-title rule))
             (map (fn [rule]
              (sug/make style-widget data {:state {:styles (:style state)
                                                           :rule rule}})) sub-rules))))))
    :on {:scrub-start (fn [e]
                        (when (= (:name e) (:name (om/get-state owner :rule)))
                          (sug/private! owner :scrubbing true)))
         :scrub-stop (fn [e]
                        (when (= (:name e) (:name (om/get-state owner :rule)))
                          (sug/fire! owner :style-set-done {})))
         :scrub (fn [e]
                  (when (= (:name e) (:name (om/get-state owner :rule)))
                    (let [dx (* (first (:diff-location e)) .5)
                          state (om/get-state owner)
                          node (om/get-node owner "input")
                          values (final/css-values (.-value node))
                          changed-values (mapv #(int (+ (:value %) dx)) values)
                          px-string (apply str (interpose " " (mapv px changed-values)))
                          input-value (if (multiple? changed-values)
                                        px-string
                                        (apply str (interpose " " changed-values)))
                          rstring (kstring (:name (:rule state)))]
                      (aset node "value" input-value)
                      (sug/fire! owner :style-change {:rule rstring :value px-string}) )))}})


(apply str (interpose " " (mapv px [11 6])))

(mapv #(int (+ (:value %) 1)) (final/css-values "10px 5px"))
