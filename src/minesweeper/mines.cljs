(ns minesweeper.mines
  (:require [clojure.set :as set]))

(defn field-slot [row col]
  {:row row
   :col col
   :mine false
   :checked false
   :marked false
   :id (str row "_" col)})

(defn slot-row [mine-index width]
  (quot mine-index width))

(defn slot-col [mine-index width]
  (rem mine-index width))

(defn make-empty-minefield [w h]
  (let [slot-indexes (range (* w h))
        mf (doall (vec (map #(field-slot (slot-row % w) (slot-col % w))
                            slot-indexes)))]
    {:game-over false
     :minecount 0
     :width w
     :height h
     :size (* w h)
     :field mf}))

(defn unique-random-numbers [n-mines mf-size]
  (let [a-set (set (take n-mines (repeatedly #(rand-int mf-size))))]
    (vec a-set)))

(defn update-slot [minefield mine-index slot-update-func &
                   minefield-update-func]
  (let [field (:field minefield)
        newslot (slot-update-func (nth field mine-index))
        newfield (doall (assoc field mine-index newslot))
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
