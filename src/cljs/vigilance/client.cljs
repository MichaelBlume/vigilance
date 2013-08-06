(ns vigilance.client
  (:require [cljs.core.async :as async
             :refer [<! >! chan timeout]]
            [cljs.core.match]
            [jayq.core :refer [$ on css]])
  (:require-macros [vigilance.macros :as macros]
                   [cljs.core.match.macros :refer [match]]
                   [cljs.core.async.macros :refer [go alt!]]))

(defn now-millis [] (.getTime (js/Date.)))

(defn time-to-fire [state interval-millis smolder-millis]
  (when (= (:state state) :waiting)
    (.log js/console "running time-to-fire")
    (let [millis-remaining (- (:end-millis state) (now-millis))
          smolder-millis-remaining (* (:fires-remaining state)
                                      smolder-millis)
          real-millis-remaining (- millis-remaining
                                   smolder-millis-remaining)
          intervals-remaining (/ real-millis-remaining interval-millis)
          r (rand-int intervals-remaining)]
      (and (> millis-remaining 0)
           (< r (:fires-remaining state))))))

(defn delayed-put [c l-millis v]
  (go (<! (timeout l-millis))
      (>! c v)))

(defn stream-in-fires [c interval fire-length smolder-length astate]
  (go
    (loop []
      (<! (timeout interval))
      (when (time-to-fire @astate interval smolder-length)
        (>! c :fire)
        (delayed-put c fire-length :end-fire)
        (delayed-put c smolder-length :end-smolder))
      (recur))))

(defn stream-in-clicks [c]
  ;; We'll see if this actually works
  (.click ($ js/document)
    (fn [_]
      (go (>! c :click)))))

(defn init-state [total-fires duration-millis]
  (let [start-millis (now-millis)]
    {:state :waiting
     :fires-remaining total-fires
     :start-millis start-millis
     :end-millis (+ start-millis duration-millis)
     :fires-missed 0
     :misfires 0
     :fires-hit 0
     :wait-millis 0}))

(defn set-screen-fire []
  (css ($ "body") "background-color" "rgb(255,0,0)"))

(defn set-screen-no-fire []
  (css ($ "body") "background-color" "rgb(255,255,255)"))

(defn missed-fire [state]
  (set-screen-no-fire)
  (-> state
    (assoc :state :smolder)
    (update-in [:fires-missed] inc)))

(defn finish-game [state]
  (js/alert
    (str
      {:misfires (:misfires state)
       :fires-missed (:fires-missed state)
       :average-delay (/ (:wait-millis state)
                         (:fires-hit state))}))
  (assoc state :state :finished))

(defn fire-click [state start-millis]
  (set-screen-no-fire)
  (-> state
    (assoc :state :smolder)
    (update-in [:fires-hit] inc)
    (update-in [:wait-millis] + (- (now-millis) start-millis))))

(defn start-fire [state]
  (do (set-screen-fire)
      (-> state
        (assoc :state [:fire (now-millis)])
        (update-in [:fires-remaining] dec))))

;; states: [[:fire start-time] :waiting :smolder :finished]
;; events: [:click :fire :end-fire :end-smolder :finish]
(defn event-handler [state event event-stream]
  (match [event (:state state)]
    [_ :finished]
         state ;; ignore

    [:finish _]
         (finish-game state)

    [:fire :waiting]
         (start-fire state)

    [:end-fire [:fire _]]
         (missed-fire state)

    [:end-fire _] state ;; ignore

    [:end-smolder :smolder]
         (assoc state :state :waiting)

    [:click [:fire start-millis]]
         (fire-click state start-millis)

    [:click _]
         (update-in state [:misfires] inc)

    [e s] (js/alert
            (str "Tell Mike he fucked some logic up."
                 [e s]))))

(defn main [{:keys [total-fires duration-millis
                    fire-length smolder-length
                    check-interval]}]
  (let [astate (atom (init-state total-fires duration-millis))
        event-stream (chan)]
    (stream-in-fires
      event-stream check-interval fire-length smolder-length astate)
    (stream-in-clicks event-stream)
    (delayed-put event-stream duration-millis :finish)
    (go
      (loop []
        (swap! astate event-handler (<! event-stream) event-stream)
        (recur)))))

(main
  {:total-fires 10
   :duration-millis 120
   :fire-length 1000
   :smolder-length 2000
   :check-interval 200})
