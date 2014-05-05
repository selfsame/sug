(ns examples.simple.core
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [clojure.string :as string]))

(enable-console-print!)



(defn- lazy-nl-via-item
  ([nl] (lazy-nl-via-item nl 0))
  ([nl n] (when (< n (. nl -length))
            (lazy-seq
             (cons (. nl (item n))
                   (lazy-nl-via-item nl (inc n)))))))

(defn- lazy-nl-via-array-ref
  ([nl] (lazy-nl-via-array-ref nl 0))
  ([nl n] (when (< n (. nl -length))
            (lazy-seq
             (cons (aget nl n)
                   (lazy-nl-via-array-ref nl (inc n)))))))

(defn- lazy-nodelist
  "A lazy seq view of a js/NodeList, or other array-like javascript things"
  [nl]
  (if (. nl -item)
    (lazy-nl-via-item nl)
    (lazy-nl-via-array-ref nl)))

(extend-type js/NodeList
    ICounted
    (-count [nodelist] (. nodelist -length))

    IIndexed
    (-nth ([nodelist n] (. nodelist (item n)))
          ([nodelist n not-found] (if (<= (. nodelist -length) n)
                                    not-found
                                    (nth nodelist n))))
    ISeqable
    (-seq [nodelist] (lazy-nodelist nodelist)))









(deftype DomCursor [value state path]

  IDeref
  (-deref [this]
    (get-in @state path))
  om/IValue
  (-value [_] value)
  om/ICursor
  (-path [_] path)
  (-state [_] state)
  om/ITransact
  (om/-transact! [this korks f tag]
    (om/transact* state this korks f tag))

  ICloneable
  (-clone [_]
    (DomCursor. value state path))
  ICounted
  (-count [_]
    (-count value))
  ICollection
;;   (-conj [_ o]
;;     (DomCursor. (-conj value o) state path))
  ILookup
  (-lookup [this k]
    (-lookup this k nil))
  (-lookup [this k not-found]
    (let [v (-lookup value k not-found)]
        (if-not (= v not-found)
          (om/to-cursor v state (conj path k))
          not-found)))
  IIndexed
  (-nth [this n]
    (om/to-cursor (-nth value n) state (conj path n)))
  (-nth [this n not-found]

      (if (< n (-count value))
        (om/to-cursor (-nth value n) state (conj path n))
        not-found))
  IFn
  (-invoke [this k]
    (-lookup this k))
  (-invoke [this k not-found]
    (-lookup this k not-found))
;;   ISeqable
;;   (-seq [this]
;;     (check
;;       (when (pos? (count value))
;;         (map (fn [[k v]] [k (-derive this v state (conj path k))]) value))))
  IAssociative
  (-contains-key? [_ k]
    (-contains-key? value k))
  (-assoc [_ k v]
    (DomCursor.  (-assoc value k v) state path))
  IMap
  (-dissoc [_ k]
    (DomCursor. (-dissoc value k) state path))
  IEquiv
  (-equiv [_ other]
      (if (om/cursor? other)
        (= value (om/-value other))
        (= value other)))
  )










(extend-type js/HTMLCollection
    ICounted
    (-count [coll] (. coll -length))

    IIndexed
    (-nth
     ([coll n] (. coll (item n)))
     ([coll n not-found] (if (<= (. coll -length) n)
                           not-found
                           (nth coll n))))
  IAssociative
  (-assoc [el n v]
    (when (and (number? n) (< -1 n (count el)))
      (aset el n v)))
  ILookup
  (-lookup [el k]
     (-lookup el k nil))
  (-lookup [el n not-found]
     (if (and (number? n) (< -1 n (count el)))
       (aget el n)
        not-found))
   ICloneable
   (-clone [this]
     this)
  ISeqable
    (-seq [coll] (lazy-nodelist coll))
  om/IToCursor
  (-to-cursor [value state]
              (DomCursor.  value state []))
    (-to-cursor [value state path]
                (DomCursor.  value state path)))




(extend-type js/Element
  ICounted
    (-count [coll] (. (.-children coll) -length))

  IIndexed
    (-nth
     ([coll n] (. (.-children coll) (item n)))
     ([coll n not-found] (if (<= (. (.-children coll) -length) n)
                           not-found
                           (nth (.-children coll) n))))
  IAssociative
  (-contains-key? [el k]
                  (let [sk (cond (keyword? k) (apply str (rest (str k)))
                   :else (str k))]
    (.hasOwnProperty el sk)))

  (-assoc [el k v]
    (let [sk (cond (keyword? k) (apply str (rest (str k)))
                   :else (str k))]
    (aset el sk v)))
  ILookup
  (-lookup [el k]
     (-lookup el k nil))
  (-lookup [el k not-found]
     (let [sk (cond (keyword? k) (apply str (rest (str k)))
                    (number? k) k
                    :else (str k))
           target (if (number? sk) (.-children el) el)
           result (aget target sk)]
       (if-not (undefined? result)
        result
        not-found)))
  ISeqable
  (-seq [c] (for [k (.keys js/Object c)
                  :when (.hasOwnProperty c k)
                  :let [v (aget c k)]]
              [k v]))
  ICloneable
  (-clone [this]
    ;(.cloneNode this true)
         this
          )

  om/IToCursor

  (-to-cursor [value state]
              ;(prn "!! -to-cursor" value state )
              (DomCursor. value state []))
  (-to-cursor [value state path]
              ;(prn "!!! -to-cursor" value state path)
              (DomCursor. value state path)))
















(range 0 3)




(defn jelement [data owner]
  (reify
    om/IInitState
    (init-state [_] {:t 0})
    om/IRenderState
    (render-state [_ state]
      (om/update-state! owner [:t] inc)
      (dom/div nil
        (dom/p nil (prn-str (om/get-state owner)))
        (dom/button #js
        {:onClick (fn [v] (.toggle (:classList (om/value data)) "red")

                    ;(om/transact! data #(identity @data))
                    )} "toggle")

        (dom/p nil (str (:localName data)

                        (when-not (= "" (:id data))
                          (str " #" (:id data) "."))
                        (string/replace (:className (om/value data)) " " ".") " "
                         (prn-str (om/-path data))
                        (prn-str (count (om/value data)))
                        (str " state:" (prn-str state)) ))



               ))))

(def APP (atom (.getElementById js/document "data")))

(om/root jelement APP  {:target (.getElementById js/document "main")})


;(string/replace "a-b-c" "-" ".")

(def c  (.getElementById js/document "data"))
(:id c)
(count c)
(nth (nth c 0) 0)
(get-in c [0 0 1])

;; (update-in {:a [{:b 5}]} [:a 0 :b] inc)

;; (update-in c [:children 0 :children 2 :children 1] clone)

;; (:type (get (:children c) 0))
;; (:children c)
;; (.log js/console (:classList c))
;; (.log js/console (:attributes c))
;; (filter (fn [[k v]] (not (or (nil? v) (= "" v)))) c)

;(:firstChild c)
