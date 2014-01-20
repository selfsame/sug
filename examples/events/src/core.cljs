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

(declare siblings)


(sug/defcomp person
  [cursor owner]

 {:init-state
   (fn [_] {} )

  :render-state
  (fn [_ state]

      (dom/div #js {:className "person"}
        (dom/div #js {:className "header"
                      :style #js {:background-color (color-str (:color state))}}
          (dom/h3 nil (:name cursor))
          (dom/button #js {:onClick (fn [e]
                  (sug/fire! owner [:sibling :my-button] {:who :me}))}
                  (str (:age state))))
        (dom/p nil (prn-str (keys (:_events state))))
        (dom/p nil (prn-str  (:message state)))
        (sug/make siblings cursor {:init-state {:color (:color state)
                                                   :age (:age state)
                                                   :parent (:parent state)}
                                      :state {:color (:color state)}})))
  :did-update
  (fn [_ _ _ _]
    (let [state (om/get-state owner)]
      (om/set-state! owner :color (mapv inc (:color state)))
      ))
  :on {:sibling (fn [e cursor owner e]
                  (let [who nil ;(:who e)
                        myname (om/get-state owner :name)]
                    ))
       :my-button
       (fn [e cursor owner]
         (let [state (om/get-state owner)
               age (:age state)]
        (om/set-state! owner :age (inc age))
        (om/set-state! owner :message (prn-str e))
        (prn [age (:color state)])
           ))}})




(sug/defcomp siblings
  [cursor owner]
   {:render-state
    (fn [_ state]
      (dom/div nil
      (sug/make-all person (:children cursor)
         {:init-state {:parent (:name cursor)
                       :age (:age state)}
          :state {:color (:color state)}})))
    :on {:sibling (fn [e] )}})



(def DATA (atom {:name "grandpa h"
                 :children
                 [{:name "mary"
                   :children
                   [{:name "sue"
                     :children
                     [{:name "bobby"
                       :children []} ]}
                    {:name "walter"
                     :children []}]}
                  {:name "john"
                   :children
                   [{:name "fred"
                     :children []}
                    {:name "donny"
                     :children []}
                    {:name "andrew"
                     :children []}]} ]}))

(sug/defcomp display-people
  [cursor owner opts]
  {:render
  (fn [_]
    (sug/make person cursor {:init-state {:age 97 :color [200 200 120]}}))
   :on{:event (fn [_ _ _] (prn "EvENT"))}})

(om/root DATA display-people (.getElementById js/document "main"))



