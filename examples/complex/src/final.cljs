(ns examples.complex.final
  (:require [goog.style :as gstyle])
  (:use [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within?
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


(css-value "..")
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

(defn _t []
  (aget js/window "_t"))

(defn box [x y w h color]
  (.draw_box (.-tracking (_t)) x y w h #js {:lineWidth 1
                                            :fillStyle "rgba(113, 183, 248, .3)";"transparent"
                                            :strokeStyle (or color "rgb(113, 183, 248)")}))


(defn update-selection [data]
  (let [app (:app-state data)
        _m (js->clj (.-_m js/window))
        selection (vec (:selection app))
        nodes (:nodes app)
        elements  (mapv :el (vals (select-keys nodes selection)))

        off [16 16];[(- (get _m "outer_x")) (- (get _m "outer_y"))]
        boxes (mapv
               (fn [el]
                 (let[[x y] (element-offset el)
                  [w h] (element-dimensions el)]
                   (get-box [x y w h]))) elements)
        bounds (bounds boxes)]
  (.clear_canvas  (.-tracking (_t)))


  (dorun (for [b boxes
               :let [{:keys [x y w h]} (offset b off)]]
    (box x y w h nil)))

    (clog (clj->js elements))
    (let [{:keys [x y b r w h]} (offset bounds off)] (box x y w h "red")
    (aset (_t) "select" (clj->js  {:selected_elements  elements :selection_box [(mapv - [x y] [16 16])
                                                                                (mapv - [r b] [16 16])]})))

    ))




