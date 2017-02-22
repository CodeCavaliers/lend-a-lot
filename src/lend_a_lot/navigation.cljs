(ns lend-a-lot.navigation
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :as re])
  (:import goog.History))

;========= Secratary Config ==========
(secretary/set-config! :prefix "#")

(defroute home-path "/" []
  (re/dispatch [:nav-to :home]))

(defroute details-path "/details/:id" [id]
  (re/dispatch [:nav-to :details id]))

(defroute new-user-path "/new" []
  (re/dispatch [:nav-to :new]))

(defroute "*" []
  (re/dispatch [:nav-to :home]))

(defonce history
  (let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))))

;========= App Handlers =========

; handler
(re/reg-event-fx :nav-to
  (fn [{:keys [db]} event]
    (let [page (nth event 1)
          extra (when (== 3 (count event)) (nth event 2))]
      {:db (assoc db :page [page extra])})))

; pages query
(re/reg-sub :pages
  (fn [db _]
    (let [[page details] (:page db)]
      {:page page
       :param details})))
