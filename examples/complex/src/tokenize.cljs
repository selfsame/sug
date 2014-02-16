(ns examples.complex.tokenize
  (:require
       [domina]
   [examples.complex.final :as final])
  (:use [examples.complex.data :only [UID]]))


(def NODES (atom {}))

(defn get-inline-style [el]
  (.getAttribute el "style"))

(defn trim-whitespace [st]
  (last (last (re-seq  #"^\s*(.*[^\s*])\s*$" st)) ))

(defn prep-rule [v]
  (when (= (count v) 3)
    (let [[_ rule value] (map trim-whitespace v)]
      {(keyword rule) value})))

(defn parse-css-text [st]
  (let [all-seq (re-seq #"([^:;]+):([^:;]+);" st)]
    (into {} (map prep-rule all-seq))))

(defn make [node uid-path]
  (.log js/console node)
  (let [uid (swap! UID inc)

         id  (aget node "id")
         tag (.toLowerCase (aget node "tagName"))
         classes (domina/classes node)
         childnodes (domina/children node)
         children (vec (map #(make % (conj uid-path uid)) childnodes))
         inline (get-inline-style node)
         css-text (final/get-style node "cssText")]
    (aset node "uid" uid)
    (aset node "uid_path" (conj uid-path uid))
    (swap! NODES #(conj % {uid {:inline (if inline (parse-css-text inline) {})
                                :cssText css-text
                                :cssMap (parse-css-text css-text)
                                :el node}}))
  {:uid uid
   :uid-path (conj uid-path uid)
   :tag tag
   :id id
   :class classes
   :children children
   :expanded true
   :node node

   }
  ))


NODES




