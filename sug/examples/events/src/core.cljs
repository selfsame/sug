

(ns examples.events.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [sug.core :refer [defcomp]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]))

(enable-console-print!)

(defn color-str [col]
  (let [[r g b] col]
  (str "rgb("r","g","b")")))

(defn rand-color []
  (let [[r g b] (repeatedly #(int (* (rand) 255)))]
    [r g b]))

(declare siblings)


(sug/defcomp person
  [cursor owner]

 {:init-state
   (fn [_] {:c2 (rand-color)} )
  :will-mount
  (fn [_]
    (let [state (om/get-state owner)
          idx (last (om/path cursor))
          width (:width state)]
      ;(om/set-state! owner :width (+ (* idx 150) left))
      ))
  :render-state
  (fn [_ state]

      (dom/div #js {:className "person" :style #js {:width (str (:width state) "%") }}
      (dom/div #js {:className "box"}
        (dom/div #js {:className "header"
                      :style #js {:background-color (color-str (:color state))}}
          (dom/h3 nil (:name cursor))
          (dom/button #js {:onClick (fn [e] (sug/fire! owner [:my-button] {:c2 (:c2 state)}))} "up")
          (dom/button #js {:onClick (fn [e] (sug/fire-both! owner [:my-button] {:c2 (:c2 state)}))} "both")
          (dom/button #js {:onClick (fn [e] (sug/fire-down! owner [:my-button] {:c2 (:c2 state)}))} "down"))
        (dom/p nil (prn-str  (om/path cursor)))
        (dom/p #js {:style #js {:background-color (:message state)}} (prn-str  (:message state))))
        (apply dom/div #js {:className "children"}
          (sug/make-all person (:children cursor) {:init-state {:color (:color state)
                                                   :age (:age state)
                                                   :parent (:parent state)
                                                   :width (/ 100 (count (:children cursor)))}
                                      :state {:color (:color state)}}))))

  :on {:sibling (fn [e cursor owner e]
                  (let [who nil ;(:who e)
                        myname (om/get-state owner :name)]
                    ))
       :my-button
       (fn [e cursor owner]
         (let [state (om/get-state owner)
               age (:age state)]
        (om/set-state! owner :age (inc age))
        (om/set-state! owner :message (color-str (:c2 e)))
        (prn [age (:color state)])
           ))}})




(def PEEPS
["albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"
 "albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"
 "albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"
 "albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"
 "albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"
 "albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"
 "albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"
 "albert" "mary" "george" "lucy" "david" "robert" "sarah"
 "marvin" "leroy" "amanda" "jason" "robbie" "james" "andrew"
 "sophie" "marcella" "reginald" "luther" "sandy" "aaron"])




(defn chain [peeps]
  (loop [names peeps result {} ]
  (if (empty? names) result
    (recur (rest names)
           {:name (first names) :children [result] }))))


(defn safe-seq? [v]
  (cond (empty? v) nil
        (sequential? v) true
        :else nil))

 (defn bb
   [v] {:name v :children [] })

 (defn aa
   ([v] (aa v []))
   ([v col]
   (assoc v :children (vec col) )))

(defn group [peeps funct]
  (loop [names peeps result [] ]
  (if (empty? names) result
    (let [n (inc (inc (rand-int 3)))
          remainder (drop n names)
          taken (take n names)]
    (recur remainder
       (vec (concat result  [(funct (first taken)  (rest taken))])))))))


(def FIRST (group (map bb PEEPS) aa))

(def TH  (group FIRST aa))

(def SEC (group TH aa))
(def HA (group SEC aa))
(def PO (group HA aa))
(def DATA HA)

(map :children DATA)

(sug/defcomp display-people
  [cursor owner opts]
  {:render
  (fn [_]
    (apply dom/div #js {:className "children"}
             (sug/make-all person cursor {:key :name :init-state {:age 97 :color [200 200 120] :width (/ 100 (count  cursor))}})
   ))
   :on{:event (fn [_ _ _] (prn "EvENT"))}})

(om/root DATA display-people (.getElementById js/document "main"))

