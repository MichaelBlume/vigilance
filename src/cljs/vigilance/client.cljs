(ns vigilance.client
  (:require [cljs.core.async :as async
             :refer [<! >! chan timeout]]
            [cljs.core.match]
            [jayq.core :refer [$ on css]])
  (:require-macros [vigilance.macros :as macros]
                   [cljs.core.match.macros :refer [match]]
                   [cljs.core.async.macros :refer [go alt!]]))

(defn time-to-fire [state]
  ;; TODO
  false)

(defn sleep [interval]
  (<! (timeout interval)))

(defn delayed-put [c l-millis v]
  (go (sleep l-millis)
      (>! c v)))

(defn stream-in-fires [c interval fire-length smolder-length astate]
  (go
    (loop []
      (sleep interval)
      (when (time-to-fire @astate)
        (>! c :fire)
        (delayed-put c fire-length :end-fire)
        (delayed-put c smolder-length :end-smolder))
      (recur))))

(defn stream-in-clicks [c]
  ;; We'll see if this actually works
  (on ($ "body") :click "" {}
    (fn [_]
      (>! c :click))))

(defn now-millis [] (.getMilliseconds (js/Date.)))

(defn init-state [total-fires duration-millis]
  (let [start-millis (now-millis)]
    {:state :waiting
     :fires-remaining total-fires
     :start-millis start-millis
     :end-time (+ start-millis duration-millis)
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
  (js/alert (str state)))

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

;; states: [[:fire start-time] :waiting :smolder]
;; events: [:click :fire :end-fire :end-smolder :finish]
(defn event-handler [event state event-stream]
  (match [event (:state state)]
    [:finish _]
         (finish-game state)

    [:fire :waiting]
         (do (set-screen-fire)
             (assoc state :state [:fire (now-millis)]))

    [:end-fire [:fire _]]
         (missed-fire state)

    [:end-fire _] state ;; ignore

    [:end-smolder :smolder]
         (assoc state :state :waiting)

    [:click [:fire start-millis]]
         (fire-click state start-millis)

    [:click _]
         (update-in state [:misfires] inc)

    [_ _] (js/alert "Tell Mike he fucked some logic up.")))

(defn main [{:keys [total-fires duration-millis
                    fire-length smolder-length]}]
  (let [astate (atom (init-state total-fires duration-millis))
        event-stream (chan)]
    (stream-in-fires
      event-stream 100 fire-length smolder-length astate)
    (stream-in-clicks event-stream)
    (delayed-put event-stream duration-millis :finish)
    (go
      (loop []
        (swap! astate event-handler (<! event-stream) event-stream)
        (recur)))))
