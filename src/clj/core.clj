(ns sug.core)

(defmacro defcomp [name-symbol args map-body ]
  (let [app (or (first args) nil)
        owner (or (first (rest args)) nil)
        opts (or (first (rest (rest args))) nil)
        init-state (or (:init-state map-body) '(fn [_] {}) )
        will-mount (or (:will-mount map-body) '(fn [_] {}) )
        did-mount (or (:did-mount map-body) '(fn [this node] ) )
        will-update (or (:will-update map-body) '(fn [this next-props next-state] ) )
        did-update (or (:did-update map-body) '(fn [this prev-props prev-state root-node] {}) )
        will-unmount (or (:will-unmount map-body) '(fn [_] {}) )
        render  (or (:render map-body) '(fn [_] {}))

        on-events (or (:on map-body) {})
        on-event-keys (keys on-events)
        on-chan-map (into {} (map (fn
                           [pair] (let [k (first pair) v (last pair)]
                           {k {:chan '(chan) :f v }} )) on-events))]

  `(defn ~name-symbol
     ;[~app ~owner ~opts]
     ~args
     (~'reify

         ~'om.core/IInitState
         (~'init-state  ~(first (rest init-state))
         ~@(rest  (rest init-state)))

        ~'om.core/IWillMount
         (~'will-mount  ~(first (rest will-mount))
         (~'om/set-state! ~owner :_events ~on-chan-map)

         ~@(rest (rest will-mount))
                        )

        ~'om.core/IDidMount
         (~'did-mount   ~(first (rest did-mount ))
         (~'-event-setup ~app ~owner)
         ~@(rest  (rest did-mount )))

       ~'om.core/IWillUpdate
         (~'will-update   ~(first (rest will-update ))
         ~@(rest  (rest will-update )))

       ~'om.core/IDidUpdate
         (~'did-update   ~(first (rest did-update ))
         ~@(rest  (rest did-update )))

      ~'om.core/IWillUnmount
         (~'will-unmount   ~(first (rest will-unmount ))
         ~@(rest  (rest will-unmount )))

      ~'om.core/IRender
         (~'render  ~(first (rest render))
         ~@(rest  (rest render)))


      ))))






;; (macroexpand '(omponent foobar
;;  { :init-state (fn [a b c]
;;                 (+ 5 5)
;;                 {:log [":init-state"] :debug (str a ", " b ", " c)})
;;   }))

 ; (defmacro defmeta [name meta value]
 ;      `(def ~(with-meta name meta) ~value))
 ;  (defmeta foo {:arglist `(list a b)} `[~'a ~'b])



 ; (defmacro pp [name-symbol f]
 ;   (let [args  `(flatten (first (rest '~f)))
 ;         body (last `'~f)
 ;         sym `~name-symbol]
 ;      `(defmeta ~sym ~{:arglist `(apply list ~args)} ~body)))

