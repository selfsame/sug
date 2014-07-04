sug
===

sugar for [Om](https://www.github.com/swannodette/om/) and core.async.

## sug/defcomp
The sug/defcomp macro takes a map instead of reified functions, and can include an optional :on map for declaring events and handler functions.

The expanded component is an om component, and you can build them with om/build or om/build-all, however if you are using sug events then you should construct components with sug/make and sug/make-all to allow the event chans to link to the children.

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
       (fn [e]
        (om/set-state! owner :active (not (om/get-state owner :active)) ))}})

(om/root label {} {:target (.-body js/document )})

```
## Events

Declaring an event handler in a component will automate linking core.async chan connections down the heirarchy, as long as child components are built with sug/make or sug/make-all.

Events can be broadcast up and down the heirarchy with `fire!` (bubbles up), `fire-down!`, and `fire-both!`.
```clj
;takes owning component, k or ks event keywords to fire at, and the map package
(sug/fire! owner [:my-button] {:c2 (:c2 state)})
```
If you want to fire down, you may need to declare a dummy event handler at the top level.

Events use core.async pub/sub to broadcast to all children, and clone/link when stepping down a generation.

It's currently not possible to 'preventDefault' on catching an event, but it's a future goal.

It's not possible to add sug events at runtime, but this is also a goal.

## private component data

sug provides a simple atomic data store for components, it's accessed like om's state, but doesn't trigger render when changed, or pending/rendered buffers.

the private data key is a combination of component's cursor path and (optional) :react-id, when multiple components share a cursor path, using unique :react-id will ensure separate scopes for their private data.

Private data persists if the component is unmounted and remounted with the same path + :react-id

```clj
;set private data
(sug/private! owner :x 5)
;get private data
(sug/private owner [:nested :data])
```

sug/private is my solution to cache trivial data like mouse position during a drag event without tripping renders all over the app. My version of the [sortable example] (https://github.com/selfsame/sug/tree/master/examples/sortable) shows it in action.
