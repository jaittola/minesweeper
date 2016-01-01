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
     :idx mine-index
     :mine false
     :checked false
     :marked false
     :adjacent-mines 0
     :id (str row "_" col)}))

(defn make-empty-minefield [w h mines-to-lay]
  (let [size (* w h)]
    {:game-over false
     :minecount 0
     :mines-to-lay mines-to-lay
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

(defn adjacent-slots [mine-index minefield]
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

(defn count-adjacent-mines [mine-index minefield]
  (->> (adjacent-slots mine-index minefield)
       (filter #(:mine %))
       (count)))

(defn setup-adjacency-counts [minefield]
  (reduce (fn [mf slot]
            (let [mine-index (:idx slot)
                  adjacent-mines (count-adjacent-mines mine-index mf)]
              (update-slot mf
                           mine-index
                           (fn [slot2]
                             (assoc slot2 :adjacent-mines adjacent-mines)))))
          minefield (:field minefield)))

(defn setup-minefield-if-needed [mine-index minefield]
  (if (> (:minecount minefield) 0)
    minefield
    (->> (setup-mines minefield (:mines-to-lay minefield) mine-index)
         (setup-adjacency-counts))))

(defn mark-adjacent-empty-slots-checked [adjacents minefield]
  (let [mf (reduce (fn [mf2 slot2]
                     (update-slot mf2 (:idx slot2)
                                  #(assoc % :checked true)))
                   minefield adjacents)
        empty-adjacents (->> (reduce
                              (fn [acc slot2]
                                (conj acc (nth (:field mf) (:idx slot2))))
                              [] adjacents)
                             (filter #(= 0 (:adjacent-mines %))))]
    (reduce (fn [mf2 slot2]
              (open-adjacent-empty-slots-if-empty
               (:idx slot2) mf2))
            mf empty-adjacents)))

(defn open-adjacent-empty-slots-if-empty [mine-index minefield]
  (let [slot (nth (:field minefield) mine-index)
        adjacents (->> minefield
                       (adjacent-slots mine-index)
                       (filter #(and (not (:mine %))
                                     (not (:checked %)))))]
    (if (or (not= 0 (:adjacent-mines slot))
            (empty? adjacents))
      minefield
      (mark-adjacent-empty-slots-checked adjacents minefield))))

(defn mines-hit [minefield]
  (filter #(and (:mine %) (:checked %)) (:field minefield)))

(defn minefield-click [mine-index minefield]
  (let [mf (update-slot minefield
                        mine-index
                        (fn [slot] (assoc slot :checked true)))]
    (assoc mf :game-over (not (empty? (mines-hit mf))))))

(defn slot-clicked [minefield row col]
  (let [mine-index (make-mine-index minefield row col)]
    (->> minefield
         (setup-minefield-if-needed mine-index)
         (minefield-click mine-index)
         (open-adjacent-empty-slots-if-empty mine-index))))

(defn unchecked-slots-without-mines [minefield]
  (->> (:field minefield)
       (filter #(and (not (:mine %)) (not (:checked %))))
       (count)))
