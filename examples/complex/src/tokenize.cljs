(ns examples.complex.tokenize
  (:require
       [domina])
  (:use [examples.complex.data :only [UID]]))


(def NODES (atom {}))

(defn make [node uid-path]
  (let [uid (swap! UID inc)

         id  (aget node "id")
         tag (.toLowerCase (aget node "tagName"))
         classes (domina/classes node)
         childnodes (domina/children node)
         children (vec (map #(make % (conj uid-path uid)) childnodes))

         ;styles (set (map TO-STYLE-ID (map style-to-string (styles node))) )
        ]
    (aset node "uid" uid)
    (aset node "uid_path" (conj uid-path uid))
    (swap! NODES #(conj % {uid {:style (domina/styles node)
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



