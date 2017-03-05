(ns lend-a-lot.navigation
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [reagent.core :as r]
            [goog.history.EventType :as EventType]
            [lend-a-lot.store :refer [state dispatch!]])
  (:import goog.History))

;========= Secratary Config ==========
(secretary/set-config! :prefix "#")

(defroute home-path "/" []
  (dispatch! [:pages :home]))

(defroute details-path "/details/:id" [id]
  (dispatch! [:pages :details id]))

(defroute new-user-path "/new" []
  (dispatch! [:pages :new]))

(defroute "*" []
  (dispatch! [:pages :home]))


(defonce history
  (let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))))

(defn nav-to! [location]
  (set! js/window.location.href location)
  nil)

(defn nav-back! []
  (. js/window.history back))

;========= App Handlers =========

(defn pages []
  (r/cursor state [:pages]))
