(ns minesweeper.core
  (:require [reagent.core :as r]
            [minesweeper.mines :as m]))

(enable-console-print!)

(println "Editing to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (r/atom (m/make-empty-minefield 5 5)))

(defn slot-clicked [row col]
  (println "Slot clicked at" row col)
  (when (not (:game-over @app-state))
    (swap! app-state m/slot-clicked row col)))

(defn render-unchecked-slot [slot]
  ^{:key (:id slot) } [:td [:a {:class "slot unchecked"
                                :on-click #(slot-clicked
                                            (:row slot)
                                            (:col slot))}]])

(defn render-mine [slot]
  ^{:key (:id slot) } [:td [:a {:class "slot mine" }]])

(defn render-explosion [slot]
  ^{:key (:id slot) } [:td [:a {:class "slot exploded-mine" }]])

(defn render-slot [slot]
  (cond
    (and (not (:checked slot)) (:mine slot)) (render-mine slot)
    (and (:checked slot) (:mine slot)) (render-explosion slot)
    :else (render-unchecked-slot slot)))

(defn render-minefield-row [row]
  (let [rnum (:row (first row))]
    ^{:key (str "r_" rnum)} [:tr (map render-slot row)]))

(defn render-minefield []
  [:div
   [:table [:tbody (map render-minefield-row (:field @app-state))]]])

(defn do-render []
  (r/render [render-minefield]
            (js/document.getElementById "container")))

(defn on-js-reload []
  (println "Reload hook")
  (do-render))

(do-render)
