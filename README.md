sug
===

sugar for [Om](https://www.github.com/swannodette/om/)

This is my attempt at sweetening the workflow for om/react. It's currently in flux but the main
rationale is to a simple convention for core.asyn chan routing between components.

```clj
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
```

## Future Ideas

I'd like to explore component mixins, perhaps mixins would have thier own namespace for their
state, and their different lifecycle code would be executed in the same order that you composed
them.
