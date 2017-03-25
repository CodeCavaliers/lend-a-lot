(ns lend-a-lot.ui
  "Common controll used in more then one place in pages."
  (:require [reagent.core :as r]
            [lend-a-lot.theme :as theme]
            [cljs-react-material-ui.reagent :as ui]))



(defn app-wrapper [{:keys [title action icon-left icon-right]} & children]
  [:div
    [ui/app-bar {:onLeftIconButtonTouchTap action
                 :title (str title)
                 :iconElementLeft icon-left
                 :iconElementRight icon-right
                 :style {:position "fixed" :top 0 :left 0}}]
    [:div {:style {:margin-top "64px"}}
      children]])

(defn flag-settings-item [setting]
  [ui/list-item {:key (:title setting)
                 :primary-text (:title setting)
                 :secondary-text (:subtitle setting)
                 :right-toggle [ui/toggle {:on-toggle (:action setting)}]}])



(defn button-spin-anim [anim element]
   [ui/css-transition-group
      {:transitionName anim
       :transitionAppear true
       :transitionAppearTimeout 25
       :transitionEnter false
       :transitionLeave false}
     element])

(defn fab [{on-click :on-click} icon]
 [ui/floating-action-button
   {:style {:position "fixed"
            :bottom "15px"
            :right "15px"
            :z-index "100"}
    :on-click on-click}
   icon])


(defn nav-button [anim icon]
 (r/as-element
   [ui/icon-button
    [button-spin-anim
     anim
     [icon
       {:color (:alternateTextColor theme/palette)}]]]))
