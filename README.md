sug
===

sugar for [Om](https://www.github.com/swannodette/om/)

Experiments with macros for om/react.

```clj
(ns examples.simple.core
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]]))

(enable-console-print!)
```
defcomp takes a map that expands into the reified om functions
:init-state :will-mount :did-mount :will-update :did-update :will-unmount :render :render-state

```clj
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
 ```
 make and make-all are just wrappers to om/build om/build-all, to automate our event channel propigation
 between elements
```clj
          (sug/make button cursor {})))
```clj
You can declare sug event handlers as follows. These will create a core.async chan, which are chained down the component heirarchy. If this component is provided a chan with matching key it will use that instead
```
  :on {:activate
       (fn [e cursor owner]
        (om/set-state! owner :active (not (om/get-state owner :active)) ))}})

(om/root {} label (.-body js/document ))
```

## Events

Events can be broadcast up and down the heirarchy with fire-up!, fire-down!, and fire-both!. (fire! is also up)
```clj
(sug/fire! owner [:my-button] {:c2 (:c2 state)})
           ;takes owning component, k or ks to fire into, and the map package

```clj

[Some notes and ideas](https://github.com/selfsame/sug/blob/master/notes.md) on the event routing.

## Future Ideas

I'd like to explore component mixins, perhaps mixins would have thier own namespace for their
state, and their different lifecycle code would be executed in the same order that you composed
them.
