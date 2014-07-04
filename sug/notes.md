Notes
===

### component state

The map returned by IInitState is merged onto the :init-state provided to
the component.

:state is merged onto component state.

## Questions

Naming my more module file "core.cljs" was messing up the dependencies, cljs.core was claiming to be my module, and goog was requiring cljs.core from my modules out/sug/ directory. Why is this?

this seems to be a reserved word, is that from js or cljs?

How should an event stay available for all listeners (unless a no-propagate is enacted)?

pub/sub can broadcast pretty easily
```clj
(defn reader [p topic nam]
  (let [c (chan)
        s (sub p topic c)]
    (go (while true (prn [nam (<! c)])  ))))

(def news (chan))
(def Times (pub news map? ))

(reader Times true :fred)   ;recieves all hashmap format messages
(reader Times :sports :sue) ;pub doesn't have this topic buddy

(put! news {:topic :sports :4 "plain"})

```

How can I organize the flow of events, and limit them (i.e. bubble up, preventDefault)?


```clj
            +-------------------+
  +         |    Broadcasts     |
  |         |    :click-uid     |
  |         |                   |
  |         |catch              |
  |         +--+--------------+-+
  |            |              |
  |     +------v---+        +-v----+
  |     |          |        |      |
  |     |          |        |      |
  v     |catch     |        |      |
        +-+-------++        +--+---+
          |       |            |
    +-----v+    +-v----+    +--v----+
    |      |    |      |    |       |
    | fire!|    |      |    |       |
    |      |    |      |    |       |
    +------+    +------+    +-------+
```

1. embedding heirarchy into the messages sent, subscribers can filter.

2. Recieve chan/pub from above.  Clone it, subscribe to the clone, subscribe the original chan to the clone pub, and send the clone downstream.  This can easily be reversed for top down event flow.

3. How to initiate a preventDefault? I don't want to wait to run the handler function between every link in the chain, perhaps you explicitly declare a handler as terminal.

Event chans could be declared with options:

```clj
:event-types {:click {:up false :down 3 ;limit the reach of the chan
                      :filter #(pos? (:y))}

              :blur {:global true}

              :mouse-move {:down 6
                           :prevent-default true
                           :}}

;on handlers without :event-types would sub to provided chans
;or create a default

:on {:click #()
     :foobar #()}

;; and we also need to create and destroy them at runtime

(sug/on :click (fn [e data owner]) {:down 10} )

```


## TODO

See if I can remove dependencies on om and core.async by using that code in the macro expansion, and assume that the user is depending on them.

Create a om/build wrapper that sneaks my :_events chan map into opts

