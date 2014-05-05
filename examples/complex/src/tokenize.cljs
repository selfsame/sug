(ns examples.complex.tokenize
  (:require
       [domina]
   [om.core :as om :include-macros true]
   [examples.complex.final :as final])
  (:use [examples.complex.data :only [UID]]))




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

(defn tokenize-style [node]
  (let [inline (get-inline-style node)]
    (if inline (parse-css-text inline) {})))



(defn make [node uid-path nodes]
  (let [found-uid  (aget node "uid")
        uid (or found-uid (keyword (str (swap! UID inc))))
        id  (aget node "id")
        tag (.toLowerCase (aget node "tagName"))
        classes (domina/classes node)
        childnodes (domina/children node)
        children (vec (map #(make % (conj uid-path uid) nodes) childnodes))
        inline (get-inline-style node)
        css-text (final/get-style node "cssText")]
    (aset node "uid" uid)
    (aset node "uid_path" (conj uid-path uid))
    (swap! nodes #(conj % {uid {:uid uid
                                :parent (last uid-path)
                                :uid-path (conj uid-path uid)
                                :tag tag
                                :id id
                                :class classes
                                :expanded true
                                :locked false
                                :hidden false
                                :children (mapv :uid children)
                                :inline (if inline (parse-css-text inline) {})
                                :cssText css-text
                                :cssMap (parse-css-text css-text)
                                :el node}}))
  {:uid uid
   :uid-path (conj uid-path uid)
   :children children}
  ))



(defn remake [data]
        (let [new-nodes (atom {})
              new-dom (make (first (.toArray (js/$$ "body"))) [] new-nodes)]
          {:dom [new-dom] :nodes @new-nodes}))


