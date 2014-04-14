(ns sug.core)


(defmacro column ([n & b]
  (let [width (if (number? n) (str n "%") "100%")
        body (if (number? n) b (cons n b)) ]
  `(~'dom/div (~'clj->js {:className "ui-column"
                          :style (~'clj->js {:width ~width}) }) ~@body))))

(defmacro row
  ([& body]
  `(~'dom/div (~'clj->js {:className "ui-row clearfix"}) ~@body)))

(defmacro label
  ([& body]
  `(~'dom/p (~'clj->js {:className "ui-label"}) ~@body)))

(defmacro group
  ([& body]
  `(~'dom/div (~'clj->js {:className "ui-group clearfix"}) ~@body)))


(defmacro have [k & body]
  `(let [~'state (~'om/get-state ~'__owner ~k)
         ~'thing (get ~'state ~k)]
     (when ~'state (do ~@body) )))

(defmacro havent [k & body]
  `(let [~'state (~'om/get-state ~'__owner ~k)
         ~'thing (get ~'state ~k)]
     (when-not ~'state (do ~@body) )))


(defmacro config [k & body]
  `(let [~'state (~'om/get-state ~'__owner)
         ~'c-edit (get-in ~'state [:customize-tool])
         ~'c-val (get-in ~'state [:custom ~k])
         ~'tog (~'sug/make ~'icon ~'data
                           {:opts {:className "config"
                                   :onClick #(~'om/update-state! ~'__owner [:custom ~k] not )}
                            :state {:x (if ~'c-val 0 -16 ) :y -80}})]



     (if ~'c-val
       (when ~'c-edit
         (~'dom/div nil ~'tog (~'dom/div (~'clj->js {:className "faded"}) ~@body)))
       (if ~'c-edit
         (~'dom/div nil ~'tog ~@body)
         ~@body) )

     ))


(defmacro make
  ([func cursor data]
  `(om/build ~func ~cursor
               (update-in ~data [:init-state :_events] merge
                          (~'om/get-state ~'__owner :_events)))))

(defmacro make-all
  [func cursor data]
  `(om/build-all ~func ~cursor
               (update-in ~data [:init-state :_events] merge
                          (~'om/get-state ~'__owner :_events))))

(defmacro defcomp [name-symbol args map-body ]
  (let [app (or (first args) nil)
        owner (or (first (rest args)) nil)
        opts (or (first (rest (rest args))) nil)

        init-state (:init-state map-body)
        will-mount (or (:will-mount map-body) '(fn [_] ))
        did-mount (or (:did-mount map-body) '(fn [this#] ))
        should-update (:should-update map-body)
        will-update (:will-update map-body)
        did-update (:did-update map-body)
        will-unmount (:will-unmount map-body)
        render  (:render map-body)
        render-state  (:render-state map-body)


        on-events (or (:on map-body) {})
        on-event-keys (keys on-events)
        on-chan-map (into {} (map (fn
                           [pair] (let [k (first pair) v (last pair)]
                           {k {:chan '(chan) :f v }} )) on-events))]

  `(defn ~name-symbol ~args
     (let [~'__owner ~owner
           ~'__opts ~opts]
     (~'reify
        ~@(when
          init-state `(
           ~'om.core/IInitState
           (~'init-state  ~(first (rest init-state))
         ~@(rest  (rest init-state)))))

        ~@(when
          will-mount `(
          ~'om.core/IWillMount
           (~'will-mount  ~(first (rest will-mount))
           (~'om/set-state! ~owner :_event_handlers ~on-events)
           (~'om/set-state! ~owner :_events
               (~'sug/-register (~'om/get-state ~owner :_events) ~on-events))

           ~@(rest (rest will-mount)))))

      ~@(when
          did-mount `(
          ~'om.core/IDidMount
           (~'did-mount   ~(first (rest did-mount ))
            (~'sug/-event-setup ~app ~owner)
         ~@(rest  (rest did-mount )))))

      ~@(when
         should-update `(
          ~'om.core/IShouldUpdate
          (~'should-update   ~(first (rest should-update ))
         ~@(rest  (rest should-update )))))

      ~@(when
         will-update `(
         ~'om.core/IWillUpdate
         (~'will-update   ~(first (rest will-update ))
         ~@(rest  (rest will-update )))))

      ~@(when
         did-update `(
         ~'om.core/IDidUpdate
         (~'did-update   ~(first (rest did-update ))
         ~@(rest  (rest did-update )))))

       ~@(when
         will-unmount `(
        ~'om.core/IWillUnmount
         (~'will-unmount   ~(first (rest will-unmount ))
         ~@(rest  (rest will-unmount )))))

       ~@(when
         render `(
        ~'om.core/IRender
         (~'render  ~(first (rest render))
         ~@(rest  (rest render)))))

      ~@(when
        render-state `(
        ~'om.core/IRenderState
         (~'render-state  ~(first (rest render-state))
         ~@(rest  (rest render-state)))))

      )  ))))





 ; (defmacro defmeta [name meta value]
 ;      `(def ~(with-meta name meta) ~value))
 ;  (defmeta foo {:arglist `(list a b)} `[~'a ~'b])


 ; (defmacro pp [name-symbol f]
 ;   (let [args  `(flatten (first (rest '~f)))
 ;         body (last `'~f)
 ;         sym `~name-symbol]
 ;      `(defmeta ~sym ~{:arglist `(apply list ~args)} ~body)))

