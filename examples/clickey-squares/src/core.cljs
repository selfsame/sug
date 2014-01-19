(ns examples.clickey-squares.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [sug.core :refer [defcomp]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true :refer []]
   [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]])
  (:use
   [examples.clickey-squares.util :only [location clog px to? from? within? get-xywh element-dimensions element-offset exclude]]))

(enable-console-print!)

(def UID (atom 0))

(defn get-UID []
  (keyword (str (swap! UID inc))))

(defn put-local [k v]
  (.setItem (aget  js/window "localStorage") k v))

(defn get-local [k]
  (.getItem (aget  js/window "localStorage") k ))

(defn atom? [x] (= (type x)(type (atom []))))


(defn pprint [thing &[ind]]
  (let [indent (or ind "")]
  (cond
    (map? thing)
    (str "{"  (string/join (str "\n" indent " ") (map (fn[p] (str (key p) "  " (pprint (val p) (str indent "  "))) ) thing) )"}")
    (vector? thing) (str "[" (string/join (str "\n" indent " ") (map (fn[a] (pprint a (str indent "  "))) thing) )"]")
   (fn? thing) :function
    (atom? thing) (pprint @thing (str indent "  "))
    :else thing)))

(defn bucket-to-key [v]
  (if (vector? v)
    (let [[x y] v]
      (keyword (str x "-" y) ))
    (prn v) ))

(defn key-to-bucket [k]
  (let [end (re-find #"[0-9]+$" (str k))
        start (re-find #"\d+[0-9]" (str k))]
    (when-not (= (count end) (count start) "") [(js/parseInt start) (js/parseInt end)] )))



(def DATA
  (atom {:buckets
          {
           (bucket-to-key [23 7]) {:uid :0 :color [112 254 78] :background-color [72 6 218]}
          } }))





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



(defcomp square
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
    (let [color (or (state owner :color)(rand-color))
          background-color (or (state owner :background-color) (color-invert color))]
    (om/transact! data #(merge % {:color color
                              :background-color background-color} ))))

 :render
 (fn [_]
   (let [uid (state owner :uid)
         box (state owner :box)]
   (dom/span #js {:style #js {:left (px (:left box))
                              :top (px (:top box))
                              :color (color-str (state owner :color))
                              :background-color (color-str (state owner :background-color))}
                  :className "foobar"
                  :onClick #(fire! owner [:box-click] {}) } uid )))

  :on {:box-click
       (fn [app owner e]
         (let [[r g b] (repeatedly #(int (* (rand) 255)))
               [ir ig ib] (map #(- 255 %) [r g b])
               c (str "rgb("r","g","b")")
               bgc (str "rgb("ir","ig","ib")")]
           (doto owner
             (state! :color (rand-color) )
             (state! :background-color (rand-color) )))) }})




(defcomp foobar
  [app owner opts]

 {:init-state
  (fn [_]
    {:box {:left 30} })

  :will-mount
  (fn [_] )

  :did-mount
  (fn [_ _]  )

  :will-update
  (fn [_ _ _]
    (when-let [to-spawn (state owner :to-spawn)]
      (let [non-dups (filter  #(not (contains? app %)) to-spawn)]
        (when (pos? (count non-dups))
          (let [
              neighbors (state owner :neighbors)
              n-col
                     (map #(:color (get app (bucket-to-key %) ) ) neighbors)
              av-col  (map #(int (* (/ 1 (count n-col)) %)) (apply map + n-col))
              new (zipmap
                   non-dups
                   (repeatedly (fn [] {:uid (get-UID)
                                       :background-color
                                       (color-wander av-col [20 20 20]) })))]

        (om/transact! app [] #(merge % new )))))))

  :did-update
  (fn [_ _ _ _])

  :will-unmount
  (fn [_] )

  :render
  (fn [_]
    (let [box (state owner :box) ]
      (when-let [to-spawn (state owner :to-spawn)]
        (state! owner :to-spawn nil))


      (dom/div #js {:className "screen"
                    :onClick (fn [e] (fire! owner [:screen-click] {:pos (location e) })) }
         (into-array
          ;(cons (dom/p #js {:className "debug" :ref "debug"} (pprint app) )
          (for [[k v] app]
           (om/build square (k app) {:key :uid :opts {:key k}})))
               )))

  :on {:screen-click
       (fn [app owner e]

         (let [b (to-bucket (:pos e))
               nei (neighbors b)
               occ (filter #(om/read app (fn [c] (contains? c (bucket-to-key %) ))  ) nei)]
          (when (pos? (count occ))
            (state! owner :to-spawn (conj (or (state owner :to-spawn) []) (bucket-to-key b) ))
            (state! owner :neighbors occ) )

         )) }})

(defn test-app [app owner opts]
  (om/build foobar (:buckets app) ) )


(om/root DATA test-app (.getElementById js/document "main"))

