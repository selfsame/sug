(ns examples.complex.tokenize
  (:require
       [domina]
   [om.core :as om :include-macros true]
   [examples.complex.final :as final])
  (:use [examples.complex.data :only [UID]]))


(defn exclude [col idx]
  (vec (concat
   (subvec col 0 idx)
   (when (<= (inc idx) (count col))
   (subvec col (inc idx) (count col))) )))

 (defn uid-path->idx-path [uid-path dom]
   (vec (interpose :children
              (loop [node dom path uid-path indicies []]
                (let [res (first (filter #(not (nil? %)) (map-indexed (fn [i n] (if (= (first path) (:uid n)) i) ) node)))]
                  (if   (or (nil? res) (= 1 (count path)))
                    (if (nil? res)
                      indicies
                      (conj indicies res ))
                    (recur (:children (get node res)) (rest path) (conj indicies res ))
                    ))))))


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
   :children children
   :expanded true}
  ))


(defn untokenize [uid nodes dom]
  "given a uid, find all descendent elements,
  remove all uid references, and delete the elements"
  (let [token (uid nodes)
        element (:el token)
        descendents (.toArray (.find (js/$ element) "*"))
        desc-uids (filter #(not (nil? %)) (map #(.-uid %) descendents))
        path (:uid-path token)
        p-uid (:parent token)
        p-token (p-uid nodes)
        dom-path (uid-path->idx-path path dom)
        purged-nodes (update-in
                      (apply dissoc nodes (conj desc-uids uid))
                      [p-uid :children] (fn [c] (vec (filter #(not= % uid) c))))
        purged-dom (update-in dom (pop dom-path) (fn [d] (exclude (vec d) (peek dom-path))))
        ]

  {:dom purged-dom :nodes purged-nodes}
  ))



(defn remake [data]
        (let [new-nodes (atom {})
              new-dom (make (first (.toArray (js/$$ "body"))) [] new-nodes)]
          {:dom [new-dom] :nodes @new-nodes}))





(defn insert-after-uid [col uid package]
  (if (nil? uid)
    (vec (cons package col))
  (let [parted (vec (partition-by #(= uid (:uid %)) (cons [] col)))]
    (if (= 1 (count parted))
      (vec (cons package (rest (first parted))))
    (vec
     (flatten
      (concat
       (subvec parted 0 2)
       [package]
       (subvec parted 2))))))))


;(insert-after-uid [{:uid :9}{:uid :1}{:uid :7}{:uid :6}] nil 'Y)

(defn register-node [node nodes dom]
  "takes a node, and inserts tokens for it in :dom and :nodes"
  (def new-nodes (atom {}))
  (let [parent (.-parentElement node)
        prev (.-previousElementSibling node)
        p-uid (.-uid parent)
        p-token (get nodes p-uid)
        p-uid-path (:uid-path p-token)
        new-dom (make node p-uid-path new-nodes)
        dom-path (uid-path->idx-path p-uid-path dom)
       altered-dom (update-in dom (conj dom-path :children) #(insert-after-uid % (:uid new-dom) new-dom))]
   {:dom altered-dom :nodes (conj nodes @new-nodes)}))





