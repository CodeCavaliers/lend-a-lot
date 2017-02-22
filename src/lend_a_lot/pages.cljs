(ns lend-a-lot.pages
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [lend-a-lot.theme :as theme]
            [re-frame.core :as re]))


(re/reg-sub :users
  (fn [db _]))

(re/reg-event-fx :new-user
  (fn [db _]))


(defn nav-to! [location]
  (set! js/window.location.href location))

(defn nav-back! []
  (. js/window.history back))

(defn button-spin-anim [anim element]
  [ui/css-transition-group
     {:transitionName anim
      :transitionAppear true
      :transitionAppearTimeout 25
      :transitionEnter false
      :transitionLeave false}
    element])

(defn nav-button [anim icon]
  (r/as-element
    [ui/icon-button
     [button-spin-anim
      anim
      [icon
        {:color (:alternateTextColor theme/palette)}]]]))

(defn fab [{on-click :on-click} icon]
  [ui/floating-action-button
    {:style {:position "absolute"
             :bottom "15px"
             :right "15px"}
     :on-click on-click}
    icon])

(defn home []
  (let [users (re/subscribe [:users])]
    [:div
      [ui/app-bar {:title "LendALot"
                   :iconElementLeft (nav-button "button-spin-left" ic/navigation-menu)}]
      [fab {:on-click #(nav-to! "#/new")}
        [ic/content-add {:color (:alternateTextColor theme/palette)}]]
      [ui/list]]))


(defn details [id]
  (let [new-user (r/atom {:user {} :valid false})]
    (fn []
      [:div
        [ui/app-bar {:onLeftIconButtonTouchTap #(nav-back!)
                     :iconElementLeft (nav-button "button-spin-right" ic/navigation-arrow-back)}]
        [fab {:on-click #(re/dispatch [:new-user (:user @new-user)])}
          [ic/content-save {:color (:alternateTextColor theme/palette)}]]
        [:div
          {:style {:padding "5px"}}
          [ui/card
            {:style {:padding "10px"}}
            [ui/text-field
              {:floating-label-text "First Name"
               :full-width true
               :on-change #(swap! new-user assoc-in [:first-name :user] (.-value (.-target %)))}]
            [ui/text-field
              {:floating-label-text "Last Name"
               :full-width true
               :on-change #(swap! new-user assoc-in [:last-name :user] (.-value (.-target %)))}]]
          [:div {:style {:height "15px"}}]
          [ui/card
            {:style {:padding "10px"}}]]])))
