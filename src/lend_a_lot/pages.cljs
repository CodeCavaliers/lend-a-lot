(ns lend-a-lot.pages
  (:require [reagent.core :as r]
            [posh.reagent :as p]
            [lend-a-lot.db :as db]
            [datascript.core :as d]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [lend-a-lot.theme :as theme]))


(defn nav-to! [location]
  (set! js/window.location.href location)
  nil)

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

(defn user-item [[user items]]
  (let [[id name] user
        items (map #(clojure.string/join " " (reverse (drop 3 %))) items)
        string (clojure.string/join ", " items)]
    [:div {:key id}
      [ui/list-item
        {:primary-text name
         :secondary-text string
         :left-avatar
            (r/as-element [ui/avatar (first name)])}]
      [ui/divider]]))


(defn home [conn]
  (let [query-result @(db/all-users conn)
        users (group-by (juxt first second) query-result)]
    [:div
      [ui/app-bar {:title "LendALot"
                   :iconElementLeft (nav-button "button-spin-left" ic/navigation-menu)}]
      [fab {:on-click #(nav-to! "#/new")}
        [ic/content-add {:color (:alternateTextColor theme/palette)}]]
      [ui/list
        {:style {:padding "0"}}
        (map user-item (seq users))]]))



(defn details [id]
  (let [new-user (r/atom {:user {} :valid false})]
    (fn []
      [:div
        [ui/app-bar {:onLeftIconButtonTouchTap #(nav-back!)
                     :iconElementLeft (nav-button "button-spin-right" ic/navigation-arrow-back)}]
        [fab {:on-click #(println "save")}
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


(defn text-field [{:keys [field atom label type value validator]}]
  (let [type (or type "text")
        error-text (@atom (keyword (str "error-" (name field))))
        validator (or validator (constantly ""))
        value  (or (@atom field) value)]
    (when value
      (swap! atom assoc field value))
    [ui/text-field
      {:floating-label-text label
       :full-width true
       :on-change #(swap! atom assoc field (.-value (.-target %)))
       :value (or value "")
       :error-text (validator value)
       :type type}]))

(defn validator [text cp value]
  (if (cp value)
    text
    ""))

(defn new-item [conn]
  (let [new-thing (r/atom {})]
    (fn []
      [:div
        [ui/app-bar {:onLeftIconButtonTouchTap #(nav-back!)
                     :iconElementLeft
                           (nav-button "button-spin-right" ic/navigation-arrow-back)
                     :iconElementRight
                           (r/as-element
                             [ui/flat-button {:label "Save"
                                              :on-click
                                                #(db/save-new-thing
                                                    conn
                                                    @new-thing)}])}]
        [:div {:style {:padding "10px"}}
          [text-field {:field :name
                       :atom new-thing
                       :validator (partial validator "This field is required" nil?)
                       :label "Name"}]
          [text-field {:field :item
                       :atom new-thing
                       :validator (partial validator "This field is required" nil?)
                       :label "Item"}]
          [text-field {:field :quantity
                       :atom new-thing
                       :label "Quantity"
                       :type "number"
                       :validator (partial validator "This fields should be at least 1" #(<= % 0))
                       :value "1"}]]])))
