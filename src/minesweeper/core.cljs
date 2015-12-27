(ns minesweeper.core
  (:require [reagent.core :as r]
            [minesweeper.mines :as m]))

(enable-console-print!)

(println "Editing to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (r/atom (m/make-empty-minefield 5 5)))
(defonce game-over (atom false))

(defn finish-game []
  (swap! game-over #(identity true))
  (swap! app-state #(assoc % :game-over true)))

(defn update-game-state [minefield]
  (swap! game-over (fn [_ mf] (:game-over mf)) minefield)
  (swap! app-state (fn [_ mf] mf) minefield))


(defn slot-clicked [row col]
  (println "Slot clicked at" row col)
  ;; This is a hack to avoid reagent's warnings about lazy
  ;; sequences. I'm not sure about the reason but this trickery
  ;; seems to work around it.
  (when (not @game-over)
    (update-game-state (m/slot-clicked @app-state row col))
    (when (= 0 (m/unchecked-slots-without-mines @app-state))
      (finish-game))))

(defn restart-clicked [size]
  (println "Restart-clicked with size" size)
  (let [n (js/Number size)]
   (when-not (js/isNaN n)
     (update-game-state (m/make-empty-minefield n n)))))

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
  (let [adj-mine-count (:adjacent-mines slot)
        adj-mine-text (if (= adj-mine-count 0) "" adj-mine-count)]
  ^{:key (:id slot) } [:td [:div {:class "slot empty" }
                            adj-mine-text]]))

(defn render-slot [slot]
  (cond
    (and @game-over (not (:checked slot)) (:mine slot)) (render-mine slot)
    (and @game-over (:checked slot) (:mine slot)) (render-explosion slot)
    (and @game-over (not (:mine slot))) (render-empty slot)
    (and (:checked slot) (not (:mine slot))) (render-empty slot)
    :else (render-unchecked-slot slot)))

(defn render-minefield-row [row]
  (let [rnum (:row (first row))]
    ^{:key (str "r_" rnum)} [:tr (map render-slot row)]))

(defn render-restart-dropdown []
  [:select {:class "restart-dropdown"
            :on-change #(restart-clicked (-> % .-target .-value))
            :value "r"}
    [:option {:value "r"} "Restart game"]
    [:option {:value "5"} "Small: 5x5"]
    [:option {:value "10"} "Medium: 10x10"]
    [:option {:value "20"} "Large: 20x20"]])

(defn render-minefield []
  (let [field (:field @app-state)
        row-vector (partition-by #(:row %) field)]
    [:div {:class "content-container"}
     [:div
      [:table [:tbody (map render-minefield-row row-vector)]]]
     (render-restart-dropdown)]))

(defn do-render []
  (r/render [render-minefield]
            (js/document.getElementById "container")))

(do-render)
