(ns clj-reactive-asteroids.core
  (:require [monet.canvas :as canvas]
            [monet.geometry :as geom]
            [reagi.core :as r]))

(enable-console-print!)

(def speed 300)
(def UP 38)
(def RIGHT 39)
(def DOWN 40)
(def LEFT 37)
(def FIRE 32)
(def PAUSE 80)
(def canvas-dom (.getElementById js/document "canvas"))
(def monet-canvas (canvas/init canvas-dom "2d"))

(defn shape-x [shape]
  (-> shape :pos deref :x))

(defn shape-y [shape]
  (-> shape :pos deref :y))

(defn shape-angle [shape]
  @(:angle shape))

(defn shape-data [x y angle]
  {:pos   (atom {:x x :y y})
   :angle (atom angle)})

(defn calculate-x [angle]
  (* speed (/ (* (Math/cos angle) Math/PI) 180)))

(defn calculate-y [angle]
  (* speed (/ (* (Math/sin angle) Math/PI) 180)))

(defn move! [shape f]
  (let [pos (:pos shape)]
    (swap! pos
     (fn [xy]
       (-> xy
         (update-in [:x]
                    #(f % (calculate-x
                            (shape-angle shape))))
         (update-in [:y]
                    #(f % (calculate-y
                            (shape-angle shape)))))))))

(defn rotate! [shape f]
  (swap! (:angle shape) #(f % (/ (/ Math/PI 3) 20))))

(defn move-forward! [shape] (move! shape +))
(defn move-backward! [shape] (move! shape -))
(defn rotate-right! [shape] (rotate! shape +))
(defn rotate-left! [shape] (rotate! shape -))


(defn ship-entity [ship]
  (canvas/entity {:x     (shape-x ship)
                  :y     (shape-y ship)
                  :angle (shape-angle ship)}

                 (fn [value]
                   (-> value
                       (assoc :x (shape-x ship))
                       (assoc :y (shape-y ship))
                       (assoc :angle (shape-angle ship))))

                 (fn [ctx val]
                   (-> ctx
                       canvas/save
                       (canvas/translate (:x val) (:y val))
                       (canvas/rotate (:angle val))
                       (canvas/begin-path)
                       (canvas/move-to 50 0)
                       (canvas/line-to 0 -15)
                       (canvas/line-to 0 15)
                       (canvas/fill)
                       canvas/restore))))


(defn make-bullet-entity [monet-canvas key shape]

  (canvas/entity {:x     (shape-x shape)
                  :y     (shape-y shape)
                  :angle (shape-angle shape)}

                 (fn [value]
                   (when (not
                           (geom/contained?
                             {:x 0 :y 0
                              :w (.-width (:canvas monet-canvas))
                              :h (.-height (:canvas monet-canvas))}
                             {:x (shape-x shape)
                              :y (shape-y shape)
                              :r 5}))
                     (canvas/remove-entity monet-canvas key))
                   (move-forward! shape)
                   (-> value
                       (assoc :x (shape-x shape))
                       (assoc :y (shape-y shape))
                       (assoc :angle (shape-angle shape))))

                 (fn [ctx val]
                   (-> ctx
                       canvas/save
                       (canvas/translate (:x val) (:y val))
                       (canvas/rotate (:angle val))
                       (canvas/fill-style "red")
                       (canvas/circle {:x 10 :y 0 :r 5})
                       (canvas/fill)
                       canvas/restore))))


(defn fire! [monet-canvas ship]
  (let [entity-key (keyword (gensym "bullet"))
        data (shape-data (shape-x ship) (shape-y ship) (shape-angle ship))
        bullet (make-bullet-entity monet-canvas entity-key data)]
    (canvas/add-entity monet-canvas entity-key bullet)))

(def ship
  (shape-data (/ (.-width (:canvas monet-canvas)) 2)
              (/ (.-height (:canvas monet-canvas)) 2)
              0))

(defn keydown-stream []
  (let [out (r/events)]
    (set! (.-onkeydown js/document)
          #(r/deliver out [::down (.-keyCode %)]))
    out))

(defn keyup-stream []
  (let [out (r/events)]
    (set! (.-onkeyup js/document)
          #(r/deliver out [::up (.-keyCode %)]))
    out))

(def active-keys-stream
  (->> (r/merge (keydown-stream) (keyup-stream))
       (r/reduce (fn [acc [event-type key-code]]
                   (condp = event-type
                     ::down (conj acc key-code)
                     ::up (disj acc key-code)
                     acc))
                 #{})
       (r/sample 15)))

(defn filter-map [pred f & args]
  (->> active-keys-stream
       (r/filter (partial some pred))
       (r/map (fn [_] (apply f args)))))

(filter-map #{FIRE} fire! monet-canvas ship)
(filter-map #{UP} move-forward! ship)
(filter-map #{DOWN} move-backward! ship)
(filter-map #{RIGHT} rotate-right! ship)
(filter-map #{LEFT} rotate-left! ship)

(defn pause! [_]
  (if @(:updating? monet-canvas)
    (canvas/stop-updating monet-canvas)
    (canvas/start-updating monet-canvas)))

(->> active-keys-stream
     (r/filter (partial some #{PAUSE}))
     (r/throttle 100)
     (r/map pause!))

(canvas/add-entity monet-canvas :ship-entity (ship-entity ship))
(canvas/draw-loop monet-canvas)