(ns examples.complex.util
  (:require [goog.style :as gstyle]
            [clojure.string :as string]
            [om.core :as om :include-macros true])
  (:use [examples.complex.data :only [UID]]))

(defn multiple? [col]
  (if (and (sequential? col)
           (< 1 (count col)))
    true false))

(defn format-keyword
  ([kw]
   (format-keyword kw {":" ""}))
  ([kw rep-map]
   (let [rm (conj {":" ""} rep-map)
         s-kw (str kw)]
     (reduce
      (fn [v [r1 r2]] (string/replace v (re-pattern r1) r2))
      s-kw rm))))


(defn- value-from-node
  [owner field]
  (let [n (om/get-node owner field)
        v (-> n .-value clojure.string/trim)]
    (when-not (empty? v)
      [v n])))

(defn- clear-nodes!
  [& nodes]
  (doall (map #(set! (.-value %) "") nodes)))

(defn location [e]
  [ (.-clientX e) (int (.-clientY e))])

(defn element-offset [el]
  (let [offset (gstyle/getPageOffset el)]
    [(.-x offset) (.-y offset)]))

(defn element-dimensions [el]
    [(.-offsetWidth el) (.-offsetHeight el)])

(defn bounding-client-rect [el]
  (let [rect (.getBoundingClientRect el)
        left (.-left rect)
        top (.-top rect)
        right (.-right rect)
        bottom (.-bottom rect)
        width (.-width rect)
        height (.-height rect)]
    {:left left :top top :right right :bottom bottom :width width :height height}))

(defn jq-dimensions [el]
  [(.width (js/$ el)) (.height (js/$ el))])

(defn get-xywh [el]
  (let [xy (element-offset el)
        wh (element-dimensions el)]
    (concat xy wh)))

(defn style! [node k v]
  (aset (.-style node) (apply str (rest (str k))) v))


(defn within? [xywh xy]
  (let [[x1 y1 w1 h1] xywh
        r1 (+ x1 w1)
        b1 (+ y1 h1)
        [x2 y2] xy]
  (if (every? true? [
    (< x2 r1)
    (> x2 x1)
    (< y2 b1)
    (> y2 y1)]) true false)))


(defn clog [thing]
  (.log js/console thing ))

(defn px [n]
  (if (number? n) (str n "px") n))

(defn descendant? [node ancestor]
  (if (.contains ancestor node) true false))


(defn to? [owner next-props next-state k]
  (or (and (not (om/get-render-state owner k))
           (k next-state))
      (and (not (k (om/get-props owner)))
           (k next-props))))

(defn from? [owner next-props next-state k]
  (or (and (om/get-render-state owner k)
           (not (k next-state)))
      (and (k (om/get-props owner))
           (not (k next-props)))))


(defn exclude [col idx]
  (vec (concat
   (subvec col 0 idx)
   (when (<= (inc idx) (count col))
   (subvec col (inc idx) (count col))) )))

(defn toggle [col v]
  (if (col v) (disj col v)
    (conj col v)))




(defn parse-file-path [path]
  (let [split (mapv first (re-seq  #"([^\/]+)" path))
        nombre (last split)
        n-split (mapv first (re-seq  #"([^.]+)" nombre))
        path-vect (vec (reverse (rest (reverse split))))
        mime (if (> (count n-split 1))
               (last n-split) nil)]
    {:file-name nombre
     :path-vect path-vect
     :path-map (reduce (fn [a b] {b {:children a}}) (reverse (conj path-vect {nombre {:file-name nombre
                                                                          :mime mime
                                                                          :file-extension mime
                                                                          :icon-link ""
                                                                          :file-size 0
                                                                          :key path}})))
     :mime mime
     :key path}))



(defn notnil? [v]
  (if (nil? v) false true))


(def script "[3x 4y 10w 10h] [6i] [7x 8y]")

(defn split-value [s]
  (let [sp (re-seq  #"(\d+)(\w+)" s)
        [vs ks] (rest (first  sp))]
    {(keyword ks) (int vs)}))

(defn get-values [s]
  (re-seq  #"\d+\w+" s))

(defn get-vects [s]
  (re-seq  #"[\[][^\]]*[\]]" s))


(map (fn [v] (into {}
                   (map split-value (get-values v)))) (get-vects script))



(defn workspace->doc [xy]
  (let [ox (aget (.-_m js/window) "scroll_x")
        oy (aget (.-_m js/window) "scroll_y")]
    (mapv - xy [ox oy] )))


(defn expanded-node [nodes uid]
  (let [node (get nodes uid)
        uid-path (:uid-path node)
        pathed (map (fn [uid] (get nodes uid)) uid-path)
        result (or
                (first (filter #(not (:expanded %)) pathed))
                (last pathed))]
    (:uid result)))


(defn put-local [k v]
  (.setItem (aget  js/window "localStorage") k v))

(defn get-local [k]
  (.getItem (aget  js/window "localStorage") k ))
