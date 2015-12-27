(ns minesweeper.mines
  (:require [clojure.set :as set]))

(defn slot-row [mine-index width]
  (quot mine-index width))

(defn slot-col [mine-index width]
  (rem mine-index width))

(defn field-slot [mine-index width]
  (let [row (slot-row mine-index width)
        col (slot-col mine-index width)]
    {:row row
     :col col
     :mine false
     :checked false
     :marked false
     :adjacent-mines 0
     :id (str row "_" col)}))

(defn make-empty-minefield [w h]
  (let [size (* w h)]
    {:game-over false
     :minecount 0
     :width w
     :height h
     :size size
     :field (doall (vec (map #(field-slot % w) (range size))))}))

(defn unique-random-numbers [n-mines mf-size]
  (let [a-set (set (take n-mines (repeatedly #(rand-int mf-size))))]
    (vec a-set)))

(defn update-slot [minefield mine-index slot-update-func &
                   [minefield-update-func]]
  (let [mf-update-func (or minefield-update-func identity)
        field (:field minefield)
        newmf (doall (->> (slot-update-func (nth field mine-index))
                          (assoc field mine-index)
                          (assoc minefield :field)))]
    (mf-update-func newmf)))

(defn set-mine-position [minefield mine-index]
  (update-slot minefield
               mine-index
               (fn [slot] (assoc slot :mine true))
               (fn [minefield] (assoc minefield
                                      :minecount
                                      (inc (:minecount minefield))))))

(defn make-mine-index [minefield row col]
  (+ (* row (:width minefield)) col))

(defn setup-mines [minefield n-mines start-pos]
  (let [mine-locations (->> (unique-random-numbers n-mines
                                                   (:size minefield))
                            (filter #(not= % start-pos)))]
    (reduce (fn [mf mine-position]
              (set-mine-position mf mine-position))
            minefield mine-locations)))

(defn adjacent-slots [minefield mine-index]
  (let [width (:width minefield)
        row (slot-row mine-index width)
        col (slot-col mine-index width)]
    (->> (:field minefield)
         (filter (fn [slot]
                   (let [r (:row slot)
                         c (:col slot)]
                     (and (<= (dec row) r (inc row))
                          (<= (dec col) c (inc col))
                          (not (and (= r row) (= c col))))))))))

(defn count-adjacent-mines [minefield mine-index]
  (->> (adjacent-slots minefield mine-index)
       (filter #(:mine %))
       (count)))

(defn setup-adjacency-counts [minefield]
  (reduce (fn [mf slot]
            (let [mine-index (make-mine-index mf (:row slot) (:col slot))
                  adjacent-mines (count-adjacent-mines mf mine-index)]
              (update-slot mf
                           mine-index
                           (fn [slot]
                             (assoc slot :adjacent-mines adjacent-mines)))))
          minefield (:field minefield)))

(defn setup-minefield-if-needed [minefield mine-index]
  (if (> (:minecount minefield) 0)
    minefield
    (let [n-mines (int (* 1.5 (:width minefield)))]
      (->> (setup-mines minefield n-mines mine-index)
           (setup-adjacency-counts)))))

(defn mines-hit [minefield]
  (filter #(and (:mine %) (:checked %)) (:field minefield)))

(defn minefield-click [minefield mine-index]
  (let [mf (update-slot minefield
                        mine-index
                        (fn [slot] (assoc slot :checked true)))]
    (assoc mf :game-over (not (empty? (mines-hit mf))))))

(defn slot-clicked [minefield row col]
  (let [mine-index (make-mine-index minefield row col)
        mf (setup-minefield-if-needed minefield mine-index)]
    (minefield-click mf mine-index)))

(defn unchecked-slots-without-mines [minefield]
  (->> (:field minefield)
       (filter #(and (not (:mine %)) (not (:checked %))))
       (count)))
