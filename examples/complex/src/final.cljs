(ns examples.complex.final
  (:require [goog.style :as gstyle])
  (:use [examples.complex.tokenize :only [tokenize-style]]
        [examples.complex.data :only [SELECTION-BOX OVER-HANDLE MOUSE-DOWN MOUSE-POS MOUSE-DOWN-POS MOUSE-TARGET]]
        [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within?
                     get-xywh element-dimensions element-offset exclude]]))

(enable-console-print!)


(defn parse-css-for [parser s]
  (let [sm
  (first (js->clj
   (try (.parse parser s) (catch js/Object e))
   ))]
    (apply conj (map (fn [[k v]] {(keyword k) v}) sm))))

(defn css-value [s]
  (parse-css-for (.-CSS_value_parser js/window) s))

(defn css-selector [s]
  (parse-css-for (.-CSS_selector_parser js/window) s))

(defn get-style [el st]
  (aget (.-style el) st))


(css-value "absolute")
(css-selector "margin-right")

(defn box? [{:keys [x y w h b r] :as value}]
  (if (or (and x y)  (and w h) (and b r)) true false))

(box? {:x 5 :y 5})

(defn point? [{:keys [x y w h b r] :as value}]
  (if (or (vector? value)
            (and x y) (and w h) (and b r)) true false))


(defmulti get-box
  (fn [x] (cond (vector? x) :vect
                  (map? x) :map

                )))
(defmethod get-box :map [box]
  (conj box (when-not (:r box) {:r (+ (:x box) (:w box))})
            (when-not (:b box) {:b (+ (:y box) (:h box))})))

(defmethod get-box :vect [box]
  (get-box (zipmap [:x :y :w :h] box)))

(defn bounds [col]
  (reduce (fn [f x]
            (let [ub (merge-with max f x)
                  lb (merge-with min f x)]
              {:x (:x lb) :y (:y lb)
               :b (:b ub) :r (:r ub)
               :w (- (:r ub) (:x lb))
               :h (- (:b ub) (:y lb))
               })) col))


(defmulti offset
  (fn [b x] (cond (vector? x) :vect
                  (map? x) :map )))

(defmethod offset :map [box xy]
  (merge-with + box (select-keys xy [:x :y :b :r])))

(defmethod offset :vect [box [x y]]
  (merge-with + box {:x x :y y :b y :r x}))



(defn element-bounds [col]
  (reduce (fn [final el]
               (let [[flb fub] final
                     [[x y][w h]] el
                     [lb ub] [[x y][(+ x w) (+ y h)]]]
                 [(mapv min flb lb) (mapv max fub ub)]
                 )) col))


(defn calc-iframe-dim []
  (let [body (js/workspace "body")
        w (.-scrollWidth body)
        h (.-scrollHeight body)]
    [w h]))





(defn _t []
  (aget js/window "_t"))

(defn box [x y w h opt]
  (let [options (if opt (clj->js opt) (clj->js {:lineWidth 1
                                       :fillStyle "rgba(113, 183, 248, .3)"
                                       :strokeStyle "rgb(113, 183, 248)"}))]
  (.draw_box (.-tracking (_t)) x y w h options)))


(defn redraw-canvas []
  (.clear_and_redraw  (.-tracking (_t))))

(defn clear-canvas []
  (.clear_canvas  (.-tracking (_t))))

(defn clear-except-rulers []
  (.clear_except_rulers  (.-tracking (_t))))

(defn draw-tracking []
  (.draw_things  (.-tracking (_t))))

(defn draw-rulers []
  (.draw_rulers  (.-tracking (_t))))

(defn update-selection [data]
  (let [app data
        _m (js->clj (.-_m js/window))
        selection (vec (:selection app))
        nodes (:nodes app)
        elements  (mapv :el (vals (select-keys nodes selection)))
        workspace-xy [(get _m "outer_x") (get _m "outer_y")]
        scroll [(get _m "scroll_x") (get _m "scroll_y")]
        off  (mapv - [16 16] scroll)
        boxes (mapv
               (fn [el]
                 (let[[x y] (element-offset el)
                  [w h] (element-dimensions el)]
                   (get-box [x y w h]))) elements)
        bounds (bounds boxes)]

  (clear-except-rulers)


  (dorun (for [b boxes
               :let [{:keys [x y w h]} (offset b off)]]
    (box x y w h nil)))

    (when (keyword? @MOUSE-TARGET)
      (let [el (:el (@MOUSE-TARGET nodes))
            [x y] (mapv + (element-offset el) off)
            [w h] (element-dimensions el)
            hover-target-box (get-box [x y w h])]
        (box x y w h {:lineWidth 2 :fillStyle "rgba(113, 183, 248, 0)" :strokeStyle "rgb(0, 0, 248, .5)"})))

    (let [{:keys [x y b r w h]} (offset bounds off)] (box x y w h "red")
    (aset (_t) "select" (clj->js  {:selected_elements  elements :selection_box [(mapv - [x y] off)
                                                                                (mapv - [r b] off)]}))
    (swap! SELECTION-BOX #(identity [(mapv + (mapv - [x y] off))
                                     (mapv + (mapv - [r b] off)) ])))

    (draw-tracking)))


(aset (.-style (.getElementById js/document "canvas_clip")) "cursor" "n-resize" )



(defn set-cursor
  ([cursor]
   (set-cursor :body cursor))
  ([target cursor]
  (cond (= target :workspace)
        (aset (.-style (js/workspace "body")) "cursor" cursor )
        (= target :body)
        (aset (.-style (.-body js/document)) "cursor" cursor ))))


(defn over-handle! [k cursor]
  (when (not= @OVER-HANDLE k)
    (swap! OVER-HANDLE #(identity k))
    (set-cursor :workspace cursor)))

(defn check-over-resize [e [x y]]
  (when @SELECTION-BOX
    (let [sb-xy (first @SELECTION-BOX)
          sb-wh (mapv - (last @SELECTION-BOX) sb-xy)
          [mid-x mid-y] (mapv - (mapv + sb-xy (mapv * sb-wh [0.5 0.5])) [5 5])
          [[sbl sbt][sbr sbb]] (mapv #(mapv - % [5 5])  @SELECTION-BOX)]

      (cond
       (within? [sbl sbt 10 10] [x y])  (over-handle! :nw-resize "nw-resize")
       (within? [sbl sbb 10 10] [x y])  (over-handle! :sw-resize "sw-resize")
       (within? [sbr sbt 10 10] [x y])  (over-handle! :ne-resize "ne-resize")
       (within? [sbr sbb 10 10] [x y])  (over-handle! :se-resize "se-resize")

       (within? [mid-x sbt 10 10] [x y])  (over-handle! :n-resize "n-resize")
       (within? [mid-x sbb 10 10] [x y])  (over-handle! :s-resize "s-resize")
       (within? [sbl mid-y 10 10] [x y])  (over-handle! :w-resize "w-resize")
       (within? [sbr mid-y 10 10] [x y])  (over-handle! :e-resize "e-resize")

       (within? (concat sb-xy sb-wh) [x y]) (over-handle! :selection "move")
       :else (over-handle! nil "default")
       ))))

(def last-mouse-pos false)

(defn nudge [tokenized [x y w h]]
  (let [node (:el tokenized)
        position (aget (.-style node) "position")]
    (when (#{"" "static"} position)
      (aset (.-style node) "position" "relative"))
    (for [[v rule] [[x "left"] [y "top"] [w "width"] [h "height"]]
          :when (not= v 0)]
      (let [inline (aget (.-style node) rule)
            computed (if (= inline "") (.css (js/$ node) rule) inline)
            parsed (if (#{"" "auto"} computed) computed (css-value computed))
            result (cond (= parsed "auto") (str v "px")
                         (= parsed "") (str v "px")
                         (map? parsed) (cond
                                        (= "px" (:unit parsed)) (str (+ (:value parsed) v) "px")
                                        :else nil)
                         :else nil)]
        (when result
          (do
            (aset (.-style node) rule result)
            {(keyword rule) result}))))))


(defn update-style-tokens [data col]
  (let [uids (if (sequential? col) col [col])
        node-data (vals
                   (select-keys
                    (get-in @data [:wrapper :app-state :nodes]) uids))]
    (dorun
           (map
            (fn [entry]
              (let [node (:el entry)
                    uid (:uid entry)]
                (swap! data
                       (fn [d] (update-in
                                d [:wrapper :app-state :nodes uid :inline]
                                #(tokenize-style node)))))) node-data))))

(defn resize-keyword? [k]
  (if (#{:n-resize :s-resize :w-resize :e-resize
             :ne-resize :se-resize :nw-resize :sw-resize} k) true false))

 (defn descendant? [token uid-set]
   (let [uid (:uid token)
         path (pop (:uid-path token))]
     (not (empty? (filter uid-set path)))))


(defn finalize-handle-interaction! [data]
  (def last-mouse-pos false)
  (def resize-cache false)
  (def hot-sb false)
  (let [uids (vec (get-in @data [:wrapper :app-state :selection]))]

    (cond (= :selection @OVER-HANDLE)
          (update-style-tokens data uids)

          (resize-keyword? @OVER-HANDLE)
          (update-style-tokens data uids))))

(defn xywh-ratio [el sb]
  (let [[[sbx sby][sbr sbb]] sb
        [sbw sbh] (mapv - [sbr sbb] [sbx sby])
        [x y] (element-offset el)
        [w h] (element-dimensions el)
        [ox oy] (mapv - [x y] [sbx sby])
        [rx ry] (mapv / [ox oy] [sbw sbh])
        [rw rh] (mapv / [w h] [sbw sbh])]
    [rx ry rw rh]))

(defn apply-resize [data members]
  (let []
    (dorun (map (fn [[token cache]]
           (let [el (:el token)
                 [x y w h] (:xywh cache)
                 offset-parent (:offsetParent cache)
                 [opx opy] (element-offset offset-parent)
                 position (aget (.-style el) "position")
                 [[sbx sby][sbr sbb]] hot-sb
                 [sbw sbh] (mapv - [sbr sbb] [sbx sby])
                 [nx ny] (mapv + (mapv * [x y] [sbw sbh]) (mapv - [sbx sby] [opx opy]))
                 [nw nh] (mapv * [w h] [sbw sbh])]
           (prn offset-parent [opx opy])
           (when (#{"" "static"} position)
            (aset (.-style el) "position" "relative"))
           (.css (js/$ el) #js {:left (px nx) :top (px ny) :width (px nw) :height (px nh) })) ) ;

         members))))





(defn handle-interaction! [data]
  (when-not last-mouse-pos
    (def last-mouse-pos @MOUSE-POS))
  (let [[dx dy] (mapv - @MOUSE-POS last-mouse-pos)
         uids (get-in @data [:wrapper :app-state :selection])

         node-data (vals
                    (select-keys
                     (get-in @data [:wrapper :app-state :nodes]) uids))
        non-descendants (filter #(not (descendant? % uids)) node-data)]

    (cond (= :selection @OVER-HANDLE)
          (do
            (let [altered-styles (into {} (mapv #(identity {(:uid %)
                                                            (apply conj (nudge % [dx dy 0 0]))}) non-descendants))]

          (update-selection (get-in @data [:wrapper :app-state]))))

          (resize-keyword? @OVER-HANDLE)
          (do
            (when-not resize-cache
              (def resize-cache
                (into {} (mapv (fn [n] {n {:xywh (xywh-ratio (:el n)  @SELECTION-BOX)
                                           :offsetParent (.-offsetParent (:el n))}}) node-data))))
            (when-not hot-sb (def hot-sb @SELECTION-BOX))
            (def hot-sb [(first hot-sb) (mapv + (last hot-sb) [dx dy])])

            (apply-resize data resize-cache)
            (update-selection (get-in @data [:wrapper :app-state]))
            ))

  (def last-mouse-pos @MOUSE-POS)))

