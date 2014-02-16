(ns examples.complex.util
  (:require [goog.style :as gstyle]
            [om.core :as om :include-macros true])
  (:use [examples.complex.data :only [UID]]))

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
    [(.-clientWidth el) (.-clientHeight el)])

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



