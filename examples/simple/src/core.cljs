(ns examples.simple.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [sug.core :refer [defcomp]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true :refer []]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]))

(enable-console-print!)

(sug/defcomp foobar
  [cursor this opts]

  ;; defcomp takes a map that expands into the reified om functions
  ;; :will-mount :did-mount :will-update :did-update :will-unmount

 {:init-state 
  (fn [_] {:active false})

  :render
  (fn [_]
      (dom/button #js 
        {:onClick (fn [e] 
            (sug/fire! this :my-button {:some 'stuff'}))} 
        (str (om/get-state this :active))))

  ;; named event handlers.  These create core.async chans, which are
  ;; passed down the component heirarchy.  
  :on {:my-button
       (fn [cursor this data] 
        (om/set-state! this :active (not (om/get-state this :active)) ))}})


(om/root {} simple (.getElementById js/document "main"))

