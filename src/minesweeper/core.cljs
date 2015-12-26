(ns minesweeper.core
  (:require [reagent.core :as r]
            [minesweeper.mines :as m]))

(enable-console-print!)

(println "Editing to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (r/atom (m/make-empty-minefield 5 5)))
(defonce game-over (atom false))

(defn slot-clicked [row col]
  (println "Slot clicked at" row col)
  ;; This is a hack to avoid reagent's warnings about lazy
  ;; sequences. I'm not sure about the reason but this trickery
  ;; seems to work around it.
  (when (not @game-over)
    (let [minefield (m/slot-clicked @app-state row col)]
      (swap! game-over (fn [_ mf] (:game-over mf)) minefield)
      (swap! app-state (fn [_ mf] mf) minefield))))

(defn render-unchecked-slot [slot]
  ^{:key (:id slot) } [:td [:a {:class "slot unchecked"
                                :on-click #(slot-clicked
                                            (:row slot)
                                            (:col slot))}]])

(defn render-mine [slot]
  ^{:key (:id slot) } [:td [:a {:class "slot mine" }]])

(defn render-explosion [slot]
  ^{:key (:id slot) } [:td [:a {:class "slot exploded-mine" }]])

(defn render-empty [slot]
  ^{:key (:id slot) } [:td [:div {:class "slot empty" }]])

(defn render-slot [slot]
  (let [gameover (:game-over @app-state)]
    (cond
      (and gameover (not (:checked slot)) (:mine slot)) (render-mine slot)
      (and gameover (:checked slot) (:mine slot)) (render-explosion slot)
      (and (:checked slot) (not (:mine slot))) (render-empty slot)
      :else (render-unchecked-slot slot))))

(defn render-minefield-row [row]
  (let [rnum (:row (first row))]
    ^{:key (str "r_" rnum)} [:tr (map render-slot row)]))

(defn render-minefield []
  (let [field (:field @app-state)
        row-vector (partition-by #(:row %) field)]
    [:div
     [:table [:tbody (map render-minefield-row row-vector)]]]))

(defn do-render []
  (r/render [render-minefield]
            (js/document.getElementById "container")))

(defn on-js-reload []
  (println "Reload hook")
  (do-render))

(do-render)
