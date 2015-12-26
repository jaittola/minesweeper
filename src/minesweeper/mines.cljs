(ns minesweeper.mines
  (:require [clojure.set :as set]))

(defn field-slot [row col]
  {:row row
   :col col
   :mine false
   :checked false
   :marked false
   :id (str row "_" col)})

(defn make-empty-minefield [w h]
  (let [mf (vec (for [row (range h)]
                  (vec (for [col (range w)]
                         (field-slot row col)))))]
    {:game-over false
     :minecount 0
     :width w
     :height h
     :size (* w h)
     :field mf}))

(defn unique-random-numbers [n-mines mf-size]
  (let [a-set (set (take n-mines (repeatedly #(rand-int mf-size))))]
    (vec a-set)))

(defn mine-pos [minefield mine-index]
  (let [width (:width minefield)
        row (quot mine-index width)
        col (rem mine-index width)]
    {:row row :col col}))

(defn update-slot [minefield mine-index slot-update-func &
                   minefield-update-func]
  (let [field (:field minefield)
        pos (mine-pos minefield mine-index)
        row (doall (nth field (:row pos)))
        slot (nth row (:col pos))
        newslot (slot-update-func slot)
        newrow (assoc row (:col pos) newslot)
        newfield (assoc field (:row pos) newrow)
        newmf (assoc minefield :field newfield)]
    (if (nil? minefield-update-func)
      newmf
      ((first minefield-update-func) newmf))))

(defn set-mine-position [minefield mine-index]
  (update-slot minefield
               mine-index
               (fn [slot] (assoc slot :mine true))
               (fn [minefield] (assoc minefield
                                      :minecount
                                      (inc (:minecount minefield))))))

(defn setup-mines [minefield n-mines start-row start-col]
  (let [mine-locations (sort (unique-random-numbers n-mines
                                                    (:size minefield)))]
    (loop [mines mine-locations
           mf minefield]
      (if (empty? mines)
        mf
        (recur (rest mines)
               (set-mine-position mf (first mines)))))))

(defn setup-minefield-if-needed [minefield row col]
  (if (<= (:minecount minefield) 0)
    (let [n-mines (int (* 1.3 (:width minefield)))]
      (setup-mines minefield n-mines row col))
    minefield))

(defn make-mine-index [row col]
  (* row col))

(defn minefield-click [minefield row col]
  (update-slot minefield
               (make-mine-index row col)
               (fn [slot] (assoc slot :checked true))))

(defn slot-clicked [minefield row col]
  (let [mf (setup-minefield-if-needed minefield row col)]
    (minefield-click mf row col)))
