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
        newmf (doall (->> (slot-update-func (nth field mine-index))
                          (assoc field mine-index)
                          (assoc minefield :field)))]
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

(defn make-mine-index [minefield row col]
  (+ (* row (:width minefield)) col))

(defn setup-mines [minefield n-mines start-row start-col]
  (let [start-pos (make-mine-index minefield start-row start-col)
        mine-locations (->> (unique-random-numbers n-mines
                                                   (:size minefield))
                            (filter #(not= % start-pos))
                            (sort))]
    (loop [mines mine-locations
           mf minefield]
      (if (empty? mines)
        mf
        (recur (rest mines)
               (set-mine-position mf (first mines)))))))

(defn setup-minefield-if-needed [minefield row col]
  (if (<= (:minecount minefield) 0)
    (let [n-mines (int (* 1.5 (:width minefield)))]
      (setup-mines minefield n-mines row col))
    minefield))

(defn mines-hit [minefield]
  (filter #(and (:mine %) (:checked %)) (:field minefield)))

(defn minefield-click [minefield row col]
  (let [mf (update-slot minefield
                        (make-mine-index minefield row col)
                        (fn [slot] (assoc slot :checked true)))]
    (assoc mf :game-over (not (empty? (mines-hit mf))))))

(defn slot-clicked [minefield row col]
  (let [mf (setup-minefield-if-needed minefield row col)]
    (minefield-click mf row col)))
