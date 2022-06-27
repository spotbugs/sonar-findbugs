(ns com.bugs.ClojureSample
  (:import (java.awt Color Container Graphics Canvas Dimension)
           (javax.swing JPanel JFrame)
           (java.awt.image BufferedImage BufferStrategy))
  (:gen-class))

(set! *warn-on-reflection* true)

(def width (float 640))
(def height (float 640))
(def max-steps (float 64))
(def color-scale (float (quot 255 max-steps)))
(def height-factor (/ 2.5 height))
(def width-factor  (/ 2.5 width))
(def bailout       (float 4.0))

(defn on-thread [^Runnable f] (.start (new Thread f)))

(defn check-bounds [cr ci]
  (loop [i  (int 1)
         zi (float 0.0)
         zr (float 0.0)]

    (let [zr2  (float (* zr zr))
          zi2  (float (* zi zi))
          temp (float (* zr zi))]
       (cond
         (> (float (+ zi2 zr2)) bailout) i

         (> i max-steps) 0

         :default (recur (inc i)
                         (float (+ (+ temp temp) ci))
                         (float (+ (- zr2 zi2) cr)))))))

(defn draw-line [^Graphics g y]
  (let [dy (float (- 1.25 (* y height-factor)))]
    (doseq [x (range 0 width)]
      (let [dx (float (- (* x width-factor) (float 2.0)))]

        (let [value   (check-bounds dx dy)
              scaled  (Math/round (float (* value color-scale)))
              xscaled (Math/round (float (* x (/ 255 width))))]

          (if (> value  0)
            (.setColor g (new Color 255 (- 255 scaled) scaled))
            (.setColor g (new Color xscaled (- 255 xscaled) xscaled)))

          (.drawRect g  x y 0 0))))))

(defn draw-lines
  ([buffer g] (draw-lines buffer g height))
  ([^BufferStrategy buffer g y]
    (doseq [y (range y)]
      (on-thread (draw-line g y))
      (.show buffer))))

(defn draw [^Canvas canvas]
  (let [buffer (.getBufferStrategy canvas)
        g      (.getDrawGraphics buffer)]
    (draw-lines buffer g)))

(defn -main []
  (let [panel  (new JPanel)
        canvas (new Canvas)
        frame  (new JFrame "Mandelbrot")]

    (doto panel
      (.setPreferredSize (new Dimension width height))
      (.setLayout nil)
      (.add canvas))

    (doto frame
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setBounds 0 0 width height)
      (.setResizable false)
      (.add panel)
      (.setVisible true))

    (doto canvas
      (.setBounds 0 0 width height)
      (.setBackground (Color/BLACK))
      (.createBufferStrategy 2)
      (.requestFocus))

    (draw canvas)))


