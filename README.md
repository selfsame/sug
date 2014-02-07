sug
===

sugar for [Om](https://www.github.com/swannodette/om/) and core.async.

Sug uses macros to automate core.async chan connections along om component heirarchies, letting you fire and handle named events.  It also uses a map structure for om lifecycle functions that expands into the reified om component.

```clj
(sug/defcomp button
  [cursor owner]
  {:render-state
  (fn [_ state]
      (dom/button #js
        {:onClick (fn [e]
            ;here we fire some data up the heirarchy
            (sug/fire! owner :activate {:some 'stuff'}))} "toggle"))})

(sug/defcomp label
  [cursor this]

 {:init-state
  (fn [_] {:active false})

  :render-state
  (fn [_ state]
      (dom/label nil
          (dom/span nil (str (:active state)))

          ;make and make-all are just wrappers to om/build om/build-all,
          ;to automate our event channel propigation between elements
          (sug/make button cursor {})))

  ;sug event chans are declared by an handler entry in the :on map. If the chan is allready existing, it will use it instead.
  :on {:activate
       (fn [e cursor owner]
        (om/set-state! owner :active (not (om/get-state owner :active)) ))}})

(om/root {} label (.-body js/document ))

```

## Events

Events can be broadcast up and down the heirarchy with `fire!` (bubbles up), `fire-down!`, and `fire-both!`. 
```clj
(sug/fire! owner [:my-button] {:c2 (:c2 state)})
           ;takes owning component, k or ks to fire into, and the map package

```

Events use core.async pub/sub to broadcast to all siblings, which clone/link when stepping down a generation.

It's currently not possible to 'preventDefault' on catching an event, but it's a future goal.

It's not possible to add sug events at runtime, but this is also a goal.

[Some notes and ideas](https://github.com/selfsame/sug/blob/master/notes.md) on the event routing.

