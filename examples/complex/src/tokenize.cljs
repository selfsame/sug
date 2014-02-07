(ns examples.complex.tokenize
  (:require
       [domina])
  (:use [examples.complex.data :only [UID]]))


(defn make [node]
  (let [uid (swap! UID inc)
         id  (aget node "id")
         tag (.toLowerCase (aget node "tagName"))
         classes (domina/classes node)

         children (vec (map make (domina/children node)))
         ;styles (set (map TO-STYLE-ID (map style-to-string (styles node))) )
        ]
  {:uid uid
   :tag tag
   :id id
   :class classes
   :children children
   :expanded true
   }
  ))



