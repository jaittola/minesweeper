(ns minesweeper.core
  (:require [reagent.core :as r]
            [minesweeper.mines :as m]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce game-difficulty (r/atom "easy"))
(defonce game-size (r/atom "5"))
(defonce game-over (atom false))
(defonce app-state (r/atom {}))

(defn finish-game []
  (swap! game-over #(identity true))
  (swap! app-state #(assoc % :game-over true)))

(defn val-for-atom [_ val]
  val)

(defn update-game-state [minefield]
  (swap! game-over (fn [_ mf] (:game-over mf)) minefield)
  (swap! app-state val-for-atom minefield))

(defn slot-clicked [row col]
  ;; This is a hack to avoid reagent's warnings about lazy
  ;; sequences. I'm not sure about the reason but this trickery
  ;; seems to work around it.
  (when (not @game-over)
    (update-game-state (m/slot-clicked @app-state row col))
    (when (= 0 (m/unchecked-slots-without-mines @app-state))
      (finish-game))))

(defn is-largish-game []
  (>= (js/Number @game-size) 10))

(defn get-mine-count [w h]
  (letfn [(easy-game [width]
            (int (* 1.5 w)))
          (medium-game [width height]
            (int (* width height 0.27)))]
    (if (or (= @game-difficulty "easy")
            (not (is-largish-game)))
      (easy-game w)
      (medium-game w h))))

(defn restart-game []
  (let [n (js/Number @game-size)]
   (when-not (js/isNaN n)
     (update-game-state (m/make-empty-minefield
                         n n
                         (get-mine-count n n))))))

(defn game-size-selected [size]
  (swap! game-size val-for-atom size)
  (restart-game))

(defn game-difficulty-selected [level]
  (swap! game-difficulty val-for-atom level)
  (restart-game))

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

(defn render-game-size-select []
  [:select {:class "game-dropdown"
            :on-change #(game-size-selected (-> % .-target .-value))
            :value @game-size}
   [:option {:value "5"} "Small: 5x5"]
   [:option {:value "10"} "Medium: 10x10"]
   [:option {:value "20"} "Large: 20x20"]])

(defn render-game-difficulty-select []
  [:select {:class "game-dropdown"
            :on-change #(game-difficulty-selected (-> % .-target .-value))
            :value @game-difficulty}
   [:option {:value "easy"} "Easy"]
   [:option {:value "medium"} "Challenging"]])

(defn render-restart-button []
  [:button {:on-click #(restart-game)} "Restart"])

(defn render-minefield []
  (let [field (:field @app-state)
        row-vector (partition-by #(:row %) field)]
    [:div {:class "content-container"}
     [:div
      [:table [:tbody (map render-minefield-row row-vector)]]]
     [:div {:class "options-container"}
      [:div {:class "restart-container"}
       (render-game-size-select)
       (when (is-largish-game)
         (render-game-difficulty-select))
       (render-restart-button)]]]))

(defn do-render []
  (r/render [render-minefield]
            (js/document.getElementById "container")))

(restart-game)
(do-render)
