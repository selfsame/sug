(ns examples.complex.commands
  (:require )
  (:use [examples.complex.data :only [UID]]
        [examples.complex.util :only [value-from-node clear-nodes! location clog px to? from? within? get-xywh element-dimensions element-offset]]))

(defn open-project []
  (prn "OPEN"))
(defn new-project []
  (prn "NEW"))
(defn undo []
  (prn "UNDO"))
(defn redo []
  (prn "REDO"))

(defn route-menu [k]
  (case k
    :open-project (open-project)
    :new-project (new-project)
    :undo (undo)
    :redo (redo)
    "default"))
