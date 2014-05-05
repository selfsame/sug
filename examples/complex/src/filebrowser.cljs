(ns examples.complex.filebrowser
(:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
      [om.core :as om :include-macros true]
      [om.dom :as dom :include-macros true]
      [sug.core :as sug :include-macros true :refer [column row label group have havent config]]
   [examples.complex.widgets :as widgets]
   [examples.complex.final :as final]
   [cljs.core.async :as async :refer [>! <! put! chan]])
  (:use
   [examples.complex.components :only [icon drop-down bool-box modal-box]]
   [examples.complex.util :only [toggle format-keyword parse-file-path value-from-node clear-nodes! location clog px style! jq-dimensions toggle
                                multiple? to? from? within? get-xywh element-dimensions element-offset get-xywh]]))



(defn load-page [path data owner]
  (let [iframe (.-ifrm js/window)]
    (.setAttribute iframe "src" path)
    (.load (js/$ iframe)
            (fn [e] (sug/fire! owner :dom-restructure {:root false})))))

(defn combine-maps [& maps]
    (apply merge-with combine-maps maps))


(defn get-local-files []
  (let [local (.-localStorage js/window)
        length (.-length local)
        files (for [idx (range 0 length)]
                (parse-file-path (.key local idx)))
        file-structure (reduce
                        (fn [a b] (combine-maps a b))
                        (map :path-map files))]
     {:children file-structure}))

(defn parse-google-drive-listing [res data owner]
  (let [items (.-items res)
        parsed (mapv (fn [i] {:file-name (.-title i)
                                 :mime (.-mimeType i)
                                 :file-extension (.-fileExtension i)
                                 :icon-link (.-iconLink i)
                                 :file-size (.-fileSize i)
                                 :kind (.-kind i)
                                 :key ""}) items)
        structure {:children (into {} (map (fn [item] {(:file-name item) item}) parsed))}]
    (om/set-state! owner :google-drive-loaded true)
    (om/transact! data [:app-state :file-systems :data :google-drive] #(identity structure))))


(defn filter-mime-fn [filt]
  (fn [col]
  (update-in col [:children] #(into {} (filter
                                        (fn [[k v]] (or (not (:file-name v))
                                                        (filt (:file-extension v)))) %1) ))))

(sug/defcomp file-node [data owner]
  {:render-state
   (fn [_ state]
     (let [hide (:hide state)
           node-name (or (:file-name data) (:folder-name state))
           children (:children data)
           has-children (and children (multiple? (keys children)))
           folder (if (or
                       (= (:mime data) "application/vnd.google-apps.folder")
                       (:file-name data)) false true)]

     (row (dom/div #js {:className "file-node "}

             (when folder
               (dom/div #js {:className "dropdown" :onClick #(om/update-state! owner [:hide] not)}
                                    (sug/make icon data {:state {:x (if hide 0 -16) :y -96}})))
                (if folder
                  (sug/make icon data {:state {:x -64 :y -64}})
                  (sug/make icon data {:state {:x -16 :y -16}}))
                (dom/p nil node-name)
              (when has-children
                (when-not hide
                  (row (column 100 (group
                    (apply dom/div nil
                      (for [k (keys (:children data) )]
                        (sug/make file-node (get (:children data) k)
                          {:init-state {:root false :hide has-children :folder-name k}
                           :state {:filter-mime (:filter-mime state)}
                           :fn (filter-mime-fn (:filter-mime state))

                           })))))))) ))))})



(sug/defcomp file-browser [data owner]
  {:init-state
   (fn [_] {:filter-mime #{"html" "jpg"}})
   :render-state
   (fn [_ state]
       (let [location (or (:location state) :local-storage)
             choices (:choices (:file-systems (:app-state data)))
             file-data (location (:data (:file-systems (:app-state data))))]

         (dom/div nil

           (dom/div #js {:className "fixed-header"}
             (group
             (column 20
               (row
               (sug/make drop-down data {:state {:active location}
                                         :opts {:onChange :change-storage-location
                                               :options choices}})))
               (column 80
                       (sug/make modal-box data {:state {:active (:filter-mime state) :options [["html" "js" "jpg" "png" "gif"]]
                                                              :onChange (fn [a] (om/update-state! owner [:filter-mime] #(toggle %1 a)))}}))))

           (dom/div #js {:className "fixed-footer"}
             (row (column 66 (dom/input #js {:ref "path"} ""))
                  (column 32 (dom/button #js {:onClick
                                                (fn [e] (let [input (om/get-node owner "path")
                                                               path (.-value input)]
                                                          (load-page path data owner)))} "load"))))
           (dom/div #js {:className "fixed-middle"}
                  (row (apply dom/div nil
             (for [k (keys (:children file-data) )]
                        (sug/make file-node (get (:children file-data) k) {:init-state {:root true}
                                            :state {:filter-mime (:filter-mime state)
                                                    :folder-name (format-keyword k {"-" " "})}
                                            :fn (filter-mime-fn (:filter-mime state))}))))))))
   :on {:change-storage-location
        (fn [e]
          (when (= (:active e) :google-drive)
            (when-not (om/get-state owner :google-drive-loaded)
              (.list (.-google (.-auth js/_t)) #(parse-google-drive-listing %1 data owner))))
          (om/set-state! owner :location (:active e)))}})


