(ns examples.complex.tokenize
  (:require
       [domina])
  (:use [examples.complex.data :only [UID]]))


(def NODES (atom {}))

(defn make [node]
  (let [uid (swap! UID inc)
         id  (aget node "id")
         tag (.toLowerCase (aget node "tagName"))
         classes (domina/classes node)
         childnodes (domina/children node)
         children (vec (map make childnodes))

         ;styles (set (map TO-STYLE-ID (map style-to-string (styles node))) )
        ]
    (aset node "uid" uid)
    (swap! NODES #(conj % {uid {:style (domina/styles node)}}))
  {:uid uid
   :tag tag
   :id id
   :class classes
   :children children
   :expanded true
   :node node

   }
  ))



