(ns examples.photoarchive.core
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true]
      [cljs.core.async :as async :refer [>! <! put! chan dropping-buffer]])
  (:use [clojure.string :only (join split)]))

(enable-console-print!)

(def DATA
  {:view-mode "medium"
   :filter ["studio"]
   :entries
  [
   {:file "444-445-TWD Artwork_Jordan Doner, New.JPG"
    :mime :jpg
    :tags #{"studio" "rhino"}}

   {:file "IMG_0949.JPG"
    :mime :jpg
    :tags #{"studio"}}

   {:file "IMG_1476.JPG" :mime :jpg
    :tags #{"studio"}}

   {:file "2_TWD31.jpg"
    :mime :jpg
    :tags #{"10 Harrison" "Thierry"}}

   {:file "3_IMG_5426.JPG"
    :mime :jpg
    :tags #{"10 Harrison"}}

   {:file "6_IMG_5596.JPG"
    :mime :jpg
    :tags #{"10 Harrison"}}

   {:file "7_IMG_5570.JPG"
    :mime :jpg
    :tags #{"10 Harrison"}}

   {:file "TWD_03_222V1.jpg"
    :mime :jpg
    :tags #{"studio" "artwork" "insect" "Laziz" "Hamani" "black"}}]})


(sug/defcomp entry
             [data owner]
             {:render-state
              (fn [_ state]
                (dom/div #js {:className "entry"}
                         (dom/img #js {:src (str "./archive/thumbs/" (:file data))})
                         (dom/p nil (:file data)))
                )})

(sug/defcomp button
  [cursor this]
  {:render-state
  (fn [_ state]
      (dom/button #js
        {:onClick (fn [e]
            (sug/fire! this :filter {:some 'stuff'}))} "search"))})

(sug/defcomp search
  [cursor this]

 {:render-state
  (fn [_ state]
      (dom/div #js {:className "search"}
          (dom/input #js {:ref "search"})

          (sug/make button cursor {})))

  :on {:filter
       (fn [e cursor owner]
         (let [node (om/get-node owner "search")
               value (. node -value)]
         (om/transact! cursor [:filter] #(split value #"\s+"))))}})

(sug/defcomp
 upload
 [data owner]
 {:render-state
  (fn [_ state]
    (dom/div #js {:className "upload"}
      (dom/button #js {:onClick #()} "upload files")
      (dom/div #js {:className "dropzone"} "drag files here"))
    )})

(sug/defcomp app
  [data owner]

  {:render-state
   (fn [_ state]
     (dom/div #js {:className "app"}
              (dom/div #js {:className "header"}
                       (dom/h1 nil "photo archive")
                       (sug/make search data {})
                       (apply dom/select #js {:onChange
                                              (fn [e]
                                                (om/transact! data [:view-mode] #(.. e -target -value)))}
                          (for [mode ["large" "medium" "small" "list"]]
                            (dom/option #js {:value mode} mode))))
              (sug/make upload data {})
              (apply dom/div #js {:className (str "entries " (:view-mode data))}
                     (sug/make-all entry (filter
                                          (fn [e]
                                            (or (or (= (:filter data) [])
                                                (false? (:filter data)))
                                                (not (empty?
                                                  (filter (:tags e) (:filter data))))) )
                                            (:entries data)) {})) ))})

(om/root DATA app (.-body js/document ))




