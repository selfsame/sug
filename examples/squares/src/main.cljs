(ns examples.squares.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [sug.core :refer [defcomp]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]])
  (:use
   [examples.squares.util :only [location px]]))

(enable-console-print!)

(def UID (atom 0))

(defn get-UID []
  (keyword (str (swap! UID inc))))


(defn bucket-to-key [v]
  (if (vector? v)
    (let [[x y] v]
      (keyword (str x "-" y) ))
    (prn v) ))

(defn key-to-bucket [k]
  (let [end (re-find #"[0-9]+$" (str k))
        start (re-find #"\d+[0-9]" (str k))]
    (when-not (= (count end) (count start) "") [(js/parseInt start) (js/parseInt end)] )))


(defn to-bucket
  ([col]
   (mapv #(int (/ % 64)) col))
  ([x y]
   (mapv #(int (/ % 64)) [x y])))

(defn neighbors [b]
  (let [[x y] b]
   (for [i [-1 0 1]
         j [-1 0 1]
         :when (not (= i j 0))]
     [(+ x i) (+ y j)] )))


(defn rand-color []
  (let [[r g b] (repeatedly #(int (* (rand) 255)))]
    [r g b]))

(defn color-str [col]
  (let [[r g b] col]
  (str "rgb("r","g","b")")))

(defn color-invert [col]
  (let [[r g b] col]
  (mapv #(- 255 %) [r g b])))

(defn color-wander [col amount]
  (let [[rc rg rb] (map #(rand-int %) amount)
        [r g b] (map + col [rc rg rb])]
  [r g b]))



(sug/defcomp square
  [data owner opts]

 {:init-state
  (fn [_]
    (let [[gx gy] (key-to-bucket (:key opts))
          [x y] (vec  (map * [64 64] [gx gy] ))
          color (or (:color data) (rand-color))
          background-color (or (:background-color data) (color-invert color))]
      {:uid (apply str (rest (str (:uid data))))
       :box {:left x :top y}
       :color color
       :background-color background-color}))

  :will-mount
  (fn [_]
    (prn "mounting:")(prn (:uid data))
    (let [color (or (sug/state owner :color)(rand-color))
          background-color (or (sug/state owner :background-color) (color-invert color))]
    (om/transact! data #(merge % {:color color
                              :background-color background-color} ))))

 :render-state
 (fn [_ state]
   (let [uid (:uid state)
         box (:box state)]
   (dom/span #js {:style #js {:left (px (:left box))
                              :top (px (:top box))
                              :color (color-str (:color state))
                              :background-color (color-str (:background-color state))}
                  :className "foobar"
                  :onClick #(sug/fire! owner [:box-click] {}) } uid )))

  :on {:box-click
       (fn [e]
         (let [[r g b] (repeatedly #(int (* (rand) 255)))
               [ir ig ib] (map #(- 255 %) [r g b])
               c (str "rgb("r","g","b")")
               bgc (str "rgb("ir","ig","ib")")]
           (doto owner
             (sug/state! :color (rand-color) )
             (sug/state! :background-color (rand-color) )))) }})




(sug/defcomp game
  [app me opts]

 {:init-state
  (fn [_] {:box {:left 30} :to-spawn []})

  :will-update
  (fn [_ _ _]
    (when-let [to-spawn (om/get-state me :to-spawn)]
      (prn to-spawn)
      (let [non-dups (filter  #(not (contains? app %)) to-spawn)]
        (when (pos? (count non-dups))
          (let [neighbors (om/get-state me :neighbors)
                n-col (map #(:color (get app (bucket-to-key %) ) ) neighbors)
                av-col  (map #(int (* (/ 1 (count n-col)) %)) (apply map + n-col))
                new (zipmap non-dups
                            (repeatedly (fn [] {:uid (get-UID)
                                                :background-color (color-wander av-col [20 20 20]) })))]
            (om/transact! app  #(merge % new )))))))

  :render-state
  (fn [_ state]
    (let [box (:box state) ]
      (when-let [to-spawn (:to-spawn state)]
        (sug/state! me :to-spawn nil))

      (apply dom/div #js {:className "screen"
                    :onClick (fn [e] (sug/fire! me [:screen-click] {:pos (location e) })) }

          (for [[k v] app]
           (sug/make square (k app) {:key :uid :opts {:key k}})))))

  :on {:screen-click
       (fn [e]
         (let [state (om/get-state me)
               b (to-bucket (:pos e))
               nei (neighbors b)
               occ (filter #(contains? @app (bucket-to-key %))  nei)]

          (when (pos? (count occ))
            (sug/state! me :to-spawn (conj (or (:to-spawn state) []) (bucket-to-key b) ))
            (sug/state! me :neighbors occ) )))}})


(def DATA
  (atom {(bucket-to-key [23 7]) {:uid :0 :color [112 254 78] :background-color [72 6 218]}}))



(om/root DATA game (.getElementById js/document "main"))

