(ns lend-a-lot.pages
  (:require [reagent.core :as r]
            [lend-a-lot.navigation :as nav]
            [lend-a-lot.store :as store :refer [dispatch!]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [lend-a-lot.theme :as theme]
            [clojure.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))


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

(defn sumarize-items [items]
  (->> items
    (sort-by #(- (:quantity %2) (:quantity %1)))
    (map #(str (:quantity %) " " (:name %)))
    (clojure.string/join ", ")))

(defn user-item [user]
  (let [name (str (:name user))
        items-summary (sumarize-items (:items user))]
    [:div {:key (:id user)}
      [ui/list-item
        {:primary-text name
         :secondary-text  items-summary
         :left-avatar (r/as-element [ui/avatar (first name)])}]
      [ui/divider]]))

(defn create-contact [js-contact]
  {:id (.-id js-contact)
   :display-name (.-displayName js-contact)})

(defn pick-contact []
  (let [contacts (.. js/window -navigator -contacts)]
    (.pickContact contacts
      (fn [c]
        (let [contact (create-contact c)]
          (nav/nav-to! "#/new")
          (dispatch! [:picked-contact contact]))))))




(defn home []
  (let [users (store/home-page @store/state)]
    [:div
      [ui/app-bar {:title "LendALot"
                   :iconElementLeft (nav-button "button-spin-left" ic/navigation-menu)}]
      [fab {:on-click #(pick-contact)}
        [ic/content-add {:color (:alternateTextColor theme/palette)}]]
      [ui/list
        {:style {:padding "0"}}
        (map user-item users)]]))



(defn details [id]
  (let [new-user (r/atom {:user {} :valid false})]
    (fn []
      [:div
        [ui/app-bar {:onLeftIconButtonTouchTap #(nav/nav-back!)
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

(defn to-data-source [users]
  (map (fn [user] {:text (:name user) :key (:id user)}) users))

(def users-datasource
  {:text :text
   :value :key})


(defn user-update-new-thing [state text data-source]
  (let [item (first (filter #(clojure.string/starts-with? (aget % "text") text) data-source))]
    (if (= text (aget item "text"))
      (swap! state assoc :user-id (aget item "key"))
      (swap! state assoc :user-id nil))))

(defn item-update-new-thing [state text data-source]
  (let [item (first (filter #(clojure.string/starts-with? % text) data-source))]
    (if (= text item)
      (do
        (swap! state assoc :item item)
        (swap! state dissoc :item-error))
      (do
        (swap! state dissoc :item)
        (swap! state assoc :item-error "Item must be selected")))))

(defn save-new-item! [{:keys [id item quantity]}]
  (when-not (some nil? [id item quantity])
    (println id item quantity)))

(defn new-item []
  (let [new-thing (r/atom {})]
    (fn []
      [:div
        [ui/app-bar {:onLeftIconButtonTouchTap #(nav/nav-back!)
                     :iconElementLeft
                           (nav-button "button-spin-right" ic/navigation-arrow-back)
                     :iconElementRight
                           (r/as-element
                             [ui/flat-button
                                {:label "Save"
                                 :on-click
                                   #(save-new-item!
                                      (assoc @new-thing :id
                                        (-> @store/state :pages :picked-contact :id)))}])}]
        [:div {:style {:padding "10px"}}
          [ui/list
            {:style {:padding "0"
                     :margin-left "-10px"
                     :margin-right "-10px"}}
            (let [name (-> @store/state :pages :picked-contact :display-name)
                  letter (first name)]
              [ui/list-item {:primary-text name
                             :disabled true
                             :right-icon-button (r/as-element [ui/icon-button
                                                                {:on-click #(pick-contact)}
                                                                [ic/editor-mode-edit]])
                             :left-avatar (r/as-element [ui/avatar letter])}])]
          [ui/auto-complete
            {:dataSource (->> (store/all-items @store/state)
                            (map :name)
                            (distinct))
             :full-width true
             :floatingLabelText "Item"
             :error-text (:item-error @new-thing)
             :onUpdateInput #(item-update-new-thing new-thing %1 %2)}]
          [text-field {:field :quantity
                       :atom new-thing
                       :label "Quantity"
                       :type "number"
                       :validator (partial validator "This fields should be at least 1" #(<= % 0))
                       :value "1"}]]])))

(defn get-page [page-name]
  (let [state store/state]
    (case page-name
      :home [home]
      :new  [new-item]
      :details [details]
      [home])))
