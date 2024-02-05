(ns example.core
  (:require
   [thi.ng.geom.core :as geom]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.glmesh :as glmesh]
   [thi.ng.geom.triangle :as tri]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.gl.shaders :as shaders]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [goog.events :as events]
   [goog.events.EventType :as EventType]
   [goog.crypt :as cry]))

(enable-console-print!)

;; (defrecord Line [begin end])
;; (defrecord Editor [buffer lines])

;; (defn allocate-buffer
;;   [size]
;;   (js/Uint8Array. size))

;; (def buf (allocate-buffer (* 1000 1000)))
;; (vec buf)

;; (defn buffer->lines
;;   [buffer]

;;   )

;; (def editor (->Editor buf [(->Line 0 9)]))

;; (:buffer editor)

;; Setup canvas
(defonce canvas (.getElementById js/document "main"))
(defn setCanvasSize []
  (doto canvas
    (.setAttribute "width" (str (.-clientWidth (.-body js/document)) "px"))
    (.setAttribute "height" (str (.-scrollHeight (.-documentElement js/document)) "px"))))
(setCanvasSize)
(.addEventListener js/window "resize" setCanvasSize)

(defonce gl-ctx (gl/gl-context "main"))

(def shader-spec
  {:vs "void main() {
          gl_Position = proj * view * vec4(position, 1.0);
       }"
   :fs "void main() {
           gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
       }"
   :uniforms {:view       :mat4
              :proj       :mat4}
   :attribs  {:position   :vec3}})

(def triangle (geom/as-mesh (tri/triangle3 [[-0.5 -0.5 0] [0.5 -0.5 0] [0 0.5 0]])
                            {:mesh (glmesh/gl-mesh 3)}))

(defonce camera (cam/perspective-camera {}))

(defn combine-model-shader-and-camera
  [model shader-spec camera]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (shaders/make-shader-from-spec gl-ctx shader-spec))
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (cam/apply camera)))

(doto gl-ctx
  (gl/clear-color-and-depth-buffer 0 0 0 1 1)
  (gl/draw-with-shader (combine-model-shader-and-camera triangle shader-spec camera)))

(def buffer (atom (js/Uint8Array. [])))
(def cursor-position (atom 0))

(defn string->bytestring
  [s]
  (cry/stringToUtf8ByteArray. s))

(defn uint8array->vector [ua]
  (vec (clj->js ua)))

(defn alphanumeric? [char]
  (let [re (js/RegExp. "^[a-zA-Z0-9]$")]
    (boolean (.test re (str char)))))

(defn white-space? [char]
  (let [re (js/RegExp. "^\\s$")]
    (boolean (.test re (str char)))))

(defn handle-keypress [event]
  (let [key (.-key event)
        char-code (int (first (string->bytestring key)))]

    (cond
      (= key "Backspace")
      (do
        (swap! buffer (fn [b] (if (seq b) (butlast b) b)))
        (when (> @cursor-position 0)
          (swap! cursor-position dec)))

      (or (alphanumeric? (char char-code)) (white-space? (char char-code)))
      (do
        (when (= (count key) 1)
          (swap! buffer #(vec (conj (uint8array->vector %) char-code)))
          (swap! cursor-position inc)))

      :else
      (println "Ignoring keypress:", key))
    (println "Buffer:" @buffer)
    (println "Buffer as characters:" (map char @buffer))
    (println "Cursor position:" @cursor-position)))

(defn setup-keyboard-listener []
  (events/listen js/window EventType/KEYDOWN handle-keypress))

(defn start-keyboard-loop []
  (println "Press keys to update the buffer. Use Backspace to remove the last character.")
  (setup-keyboard-listener))

(start-keyboard-loop)

(comment
  ;; figwheel-commands
  ;; (figwheel.main/reset)  ;; stops, cleans, reloads config, and starts autobuilder
  ;; (figwheel.main/status) ;; displays current state of system

  (vec (cry/stringToUtf8ByteArray. "asd"))
  (int \a)

  (doto gl-ctx
    (gl/clear-color-and-depth-buffer 0 0 0 1 1)
    (gl/draw-with-shader (combine-model-shader-and-camera (geom/as-mesh () {:mesh (glmesh/gl-mesh 3)}) shader-spec camera)))
  ;; WebGL
  (doto gl-ctx
    (gl/clear-color-and-depth-buffer 0 0 0 0.5 1)) ;; Set ctx to grey.
  (prn camera))
