sug
===

sugar for [Om](https://www.github.com/swannodette/om/)

This is my attempt at sweetening the workflow for om/react. It's currently in flux but the main
rationale is to a simple convention for core.asyn chan routing between components.

```clj
(defcomp foobar
  [app owner opts]

  ;; sug/defcomp takes a map that expands into the reified om functions

 {:init-state (fn [_] {})
  :will-mount (fn [_])
  :did-mount (fn [_ _])
  :will-update (fn [_ _ _])
  :did-update (fn [_ _ _ _])
  :will-unmount (fn [_] )

  :render
  (fn [_]
      (dom/button #js 
      	{:onClick (fn [e] 
      		(fire! owner :my-button {:some 'stuff'}))} "clickit"))

  ;; named event handlers.  These create core.async chans, which are
  ;; passed down the component heirarchy.  
  :on {:my-button
       (fn [app owner e] (js/alert "event recieved")) }})
```

## Future Ideas

I'd like to explore component mixins, perhaps mixins would have thier own namespace for their
state, and their different lifecycle code would be executed in the same order that you composed
them.
