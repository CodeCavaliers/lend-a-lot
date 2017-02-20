(ns lend-a-lot.pages
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [lend-a-lot.theme :as theme]
            [re-frame.core :as re]))

(defn users-query [db _]
  (vals (:users db)))

(re/reg-sub :users users-query)

(defn nav-to! [location]
  (set! js/window.location.href location))

(defn nav-back! []
  (. js/window.history back))

(defn button-spin-anim [element]
  [ui/css-transition-group
     {:transitionName "button-spin"
      :transitionAppear true
      :transitionAppearTimeout 25
      :transitionEnter false
      :transitionLeave false}
    element])

(defn nav-button [icon]
  (r/as-element
    [ui/icon-button
     [button-spin-anim
      [icon
        {:color (:alternateTextColor theme/palette)}]]]))

(defn home []
  (let [users (re/subscribe [:users])]
    [:div
      [ui/app-bar {:title "LendALot"
                   :iconElementLeft (nav-button ic/navigation-menu)}]

      [ui/floating-action-button
        {:style {:position "absolute"
                 :bottom "15px"
                 :right "15px"}
         :on-click #(nav-to! "#/new")}
        [ic/content-add {:color (:alternateTextColor theme/palette)}]]]))


(defn details [id]
  [:div
    [ui/app-bar {:onLeftIconButtonTouchTap #(nav-back!)
                 :iconElementLeft (nav-button ic/navigation-arrow-back)}]
    [:div
      {:style {:padding "5px"}}
      [ui/card
        {:style {:padding "10px"}}
        [ui/text-field
          {:floating-label-text "First Name"
           :full-width true}]
        [ui/text-field
          {:floating-label-text "Last Name"
           :full-width true}]]
      [:div {:style {:height "15px"}}]
      [ui/card
        {:style {:padding "10px"}}]]])
