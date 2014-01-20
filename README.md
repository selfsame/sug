sug
===

sugar for [Om](https://www.github.com/swannodette/om/)

This is my attempt at sweetening the workflow for om/react. The main goal is to simplify
core.async chan routing between components that map to keys for event firing and handling.

```clj
(ns examples.simple.core
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]))

(enable-console-print!)

;; defcomp takes a map that expands into the reified om functions
;; :init-state :will-mount :did-mount :will-update :did-update :will-unmount :render :render-state

(sug/defcomp button
  [cursor this]
  {:render-state
  (fn [_ state]
      (dom/button #js
        {:onClick (fn [e]
            (sug/fire! this :activate {:some 'stuff'}))} "toggle"))})

(sug/defcomp label
  [cursor this]

 {:init-state
  (fn [_] {:active false})

  :render-state
  (fn [_ state]
      (dom/label nil
          (dom/span nil (str (:active state)))

          ;make and make-all are just wrappers to om/build om/build-all,
          ;which sneaks our async chans to descendants

          (sug/make button cursor {})))

  ;; named event handlers.  These create core.async chans, which are
  ;; chained down the component heirarchy.

  :on {:activate
       (fn [e cursor owner]
        (om/set-state! owner :active (not (om/get-state owner :active)) ))}})

(om/root {} label (.-body js/document ))
```

## Future Ideas

I'd like to explore component mixins, perhaps mixins would have thier own namespace for their
state, and their different lifecycle code would be executed in the same order that you composed
them.
