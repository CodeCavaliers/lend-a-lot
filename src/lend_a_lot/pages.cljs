(ns lend-a-lot.pages
  (:require [reagent.core :as r]
            [lend-a-lot.navigation :as nav]
            [lend-a-lot.effect-processor :refer [dispatch!]]
            [lend-a-lot.reactions :as reactions]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [lend-a-lot.theme :as theme]
            [clojure.string :as str]
            [lend-a-lot.ui :as lui]))


(defn sumarize-items
  "Creates a sumarized version for a list of items in format:
  <quantity> <name>, <quantity> <name>

  Ex:
    2 Tigari, 1 Cablu Telefon

  Params:
    prop - the prop used for 'name'
    items - a list of items (maps of :quantity and :name)
  Result:
    a formated string"
  [prop items]
  (->> items
    (sort-by #(- (:quantity %2) (:quantity %1)))
    (map #(str (:quantity %) " " (prop %)))
    (clojure.string/join ", ")))

(comment
  var stringHexNumber =
    parseInt()
        parseInt('mj3bPTCbIAVoNr93me1I', 36)
            .toExponential()
            .slice(2,-5)
    , 10 & 0xFFFFFF
  .toString(16).toUpperCase())


(defn pad [string c char]
  (let [length (count string)]
    (if (>= length c)
      string
      (str string (str/join "" (repeat (- c length) char))))))



(defn string-to-hex [string]
  (str "#" (-> string
              (pad 7 "X")
              (.split " ")
              (.join "")
              (js/parseInt 36)
              (.toExponential)
              (.slice 2 -5)
              (js/parseInt 10)
              (bit-and 0xFFFFFF)
              (.toString 16)
              (.toUpperCase))))

(defn avatar [user]
  (let [photo-url (:photo-url user)]
    (if photo-url
      (r/as-element [ui/avatar {:src photo-url}])
      (r/as-element [ui/avatar {:background-color (string-to-hex (:name user))}
                        (first (:name user))]))))

(defn user-item
  "UI representation of a user for the home page list"
  [user]
  (let [name (str (:name user))
        items-summary (sumarize-items :name (:items user))]
    [:div {:key (:id user)}
      [ui/list-item
        {:primary-text name
         :on-click #(dispatch! [:nav-to (str "#/details/" (:id user))])
         :secondary-text  items-summary
         :left-avatar (avatar user)}]
      [ui/divider]]))

(defn item-list-item
  "UI representation of a user for the home page list"
  [user]
  (let [name (str (:item-name user))
        items-summary (sumarize-items :user-name (:users user))]
    [:div {:key (:id user)}
      [ui/list-item
        {:primary-text name
         :on-click #(dispatch! [:nav-to (str "#/details-by-item/" name)])
         :secondary-text  items-summary
         :left-avatar (r/as-element [ui/avatar
                                      {:background-color (string-to-hex name)}
                                      (first name)])}]
      [ui/divider]]))





(defn settings-menu []
  (let [by-user (-> @reactions/settings :group-by-user)]
    [ui/list
      [ui/subheader "Settings"]
      [ui/list-item
        {:primary-text "List Grouping"
         :secondary-text (if by-user
                          "Group By User"
                          "Group By Item")
         :right-toggle
            (r/as-element [ui/toggle
                            {:on-toggle
                              #(dispatch!
                                  [:settings/group-by-user (not by-user)])}])}]
      [ui/subheader "Sync"]
      [ui/list-item
        {:primary-text "Backup"}]
      [ui/list-item
        {:primary-text "Restore"}]]))


(defn users-filter-fn [value item]
  (let [name (:name item)
        users (:items item)]
    (or (str/includes? name value)
        (some (fn [user] (str/includes? (:name user) value)) users))))

(defn items-filter-fn [value item]
  (let [name (:item-name item)
        users (:users item)]
    (or (str/includes? name value)
        (some (fn [user] (str/includes? (:user-name user) value)) users))))

(defn home-page [list-data list-item-fn filter-fn]
  (let [list-filter (:list-filter @reactions/db)
        filtered-list-data (filter (partial filter-fn list-filter) list-data)
        drawer-state (:drawer-open @reactions/db)]
    [lui/app-wrapper
      {:title "LendALot"
       :action #(dispatch! [:drawer (not drawer-state)])
       :icon-left (lui/nav-button "button-spin-left" ic/navigation-menu)}
      (when (:loaded @reactions/db)
        [lui/fab {:on-click #(dispatch! [:start-contact-picker])}
          [ic/content-add {:color (:alternateTextColor theme/palette)}]])
      [ui/drawer
        {:docked false
         :open drawer-state
         :onRequestChange #(dispatch! [:drawer (not drawer-state)])}
        [settings-menu]]
      (if-not (:loaded @reactions/db)
        [ui/circular-progress {:innerStyle {:position "fixed" :top "50%" :left "50%" :margin-top "-20px" :margin-left "-20px"}}]
        [:div {:style {:overflow "auto"
                       :margin-top "64px"}}
          [ui/text-field {:full-width true
                          :value (or list-filter "")
                          :on-change #(dispatch! [:list-filter (.-value (.-target %))])
                          :hint-text "Enter a contanct name or item."
                          :style {:padding-top "5px"
                                  :padding-bottom "5px"}}]
          [ic/action-search {:style {:position "absolute"
                                     :top "78px"
                                     :right "20px"}}]
          [ui/list
            {:style {:padding "0"}}
            (map list-item-fn filtered-list-data)]])]))

(defn home
  "The home page."
  []
  (if (-> @reactions/settings :group-by-user)
    [home-page @reactions/home-page-by-users user-item users-filter-fn]
    [home-page @reactions/home-page-by-items item-list-item items-filter-fn]))


(defn details-list-item [user]
  [:div {:key (:id user)}
    [ui/list-item {:primary-text (:name user)
                   :secondary-text (str "Quantity: " (:quantity user))
                   :disabled true}
      [:div {:style {:float "right"
                     :margin-top "-7px"}}
        [ui/icon-button
          {:touch true
           :on-touch-tap #(dispatch! [:update-item-quantity (:id user) 0])}
          [ic/content-clear]]
        [ui/icon-button
          {:touch true
           :on-touch-tap
              #(dispatch! [:update-item-quantity
                           (:id user)
                           (-> user :quantity dec)])}
          [ic/content-remove]]
        [ui/icon-button
          {:touch true
           :on-touch-tap
              #(dispatch! [:update-item-quantity
                           (:id user)
                           (-> user :quantity inc)])}
          [ic/content-add]]]]
    [ui/divider]])

(defn details []
  (let [user @reactions/user-details]
    [lui/app-wrapper
      {:title "Edit"
       :action #(dispatch! [:nav-back])
       :icon-left (lui/nav-button "button-spin-right" ic/navigation-arrow-back)}
      [lui/fab {:on-click #(dispatch! [:add-to-existing-user user])}
         [ic/content-add {:color (:alternateTextColor theme/palette)}]]
      [ui/list
           {:style {:padding "0"}}
           (let [name (:name user)
                 letter (first name)]
             [ui/list-item {:primary-text name
                            :disabled true
                            :left-avatar (avatar user)}])]
      [:div {:style {:padding "0"
                     :overflow "auto"}}
        (map details-list-item (:items user))
        [:div {:style {:height "75px"}}]]]))


(defn item-details-update-handler
  "When there are no more users associated with an item nav back to main view.
  Because the item gets filtered up and should not longer tehnically exist.

  The function checks if the last users' items quantity is set to zero
  if true then it dispatches a nav-back action."
  [item-id quantity]
  (dispatch! [:update-item-quantity item-id quantity])
  (when (and (= quantity 0)
             (= 1 (count (:users @reactions/item-details))))
    (dispatch! [:nav-back])))


(defn item-details-list-item [user]
  [:div {:key (:gen-id user)}
    [ui/list-item {:primary-text (:user-name user)
                   :secondary-text (str "Quantity: " (:quantity user))
                   :disabled true}
      [:div {:style {:float "right"
                     :margin-top "-7px"}}
        [ui/icon-button
          {:touch true
           :on-touch-tap #(item-details-update-handler (:id user) 0)}
          [ic/content-clear]]
        [ui/icon-button
          {:touch true
           :on-touch-tap
              #(item-details-update-handler
                           (:id user)
                           (-> user :quantity dec))}
          [ic/content-remove]]
        [ui/icon-button
          {:touch true
           :on-touch-tap
              #(item-details-update-handler
                           (:id user)
                           (-> user :quantity inc))}
          [ic/content-add]]]]
    [ui/divider]])

(defn details-by-item []
  (let [item @reactions/item-details]
    [lui/app-wrapper
      {:title "Edit"
       :action #(dispatch! [:nav-back])
       :icon-left (lui/nav-button "button-spin-right" ic/navigation-arrow-back)}
      [lui/fab {:on-click #(dispatch! [:add-user-to-item item])}
        [ic/content-add {:color (:alternateTextColor theme/palette)}]]
      [ui/list
           {:style {:padding "0"}}
           (let [name (:item-name item)
                 letter (first name)]
             [ui/list-item {:primary-text name
                            :disabled true
                            :left-avatar (r/as-element [ui/avatar
                                                          {:background-color (string-to-hex name)} letter])}])]
      [:div {:style {:padding "0"
                     :overflow "auto"}}
          (map item-details-list-item (:users item))
          [:div {:style {:height "75px"}}]]]))




(defn text-field
  "A wrapper for ui/text-field that validates and auto updates an atom with value"
  [{:keys [field atom label type value validator]}]
  (let [type (or type "text")
        error-text (@atom (keyword (str "error-" (name field))))
        validator (or validator (constantly ""))
        value  (or (@atom field) value)]
    (when value
      (swap! atom assoc field value))
    [ui/text-field
      {:floating-label-text label
       :full-width true
       :on-change #(swap! atom assoc field (js/parseInt (.-value (.-target %))))
       :value (or value "")
       :error-text (validator value)
       :type type}]))

(defn validator
  "A simple validation text function.
  Returns error text if (cp value) returns true."
  [text cp value]
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
      (swap! state assoc :item item)
      (swap! state assoc :item text))))

(defn save-new-item! [c]
  (let [{:keys [contact item quantity]} c
        {:keys [id name photo-url]} contact]
    (when-not (some nil? [id item quantity])
      (dispatch! [:save-new-item id name item quantity photo-url]))))



(defn new-item []
  (let [new-thing (r/atom {:item (-> @reactions/pages :param)})]
    (fn []
        [lui/app-wrapper
          {:action #(dispatch! [:nav-back])
           :icon-left (lui/nav-button "button-spin-right" ic/navigation-arrow-back)
           :icon-right (r/as-element
                         [ui/flat-button
                            {:label "Save"
                             :on-click
                               #(save-new-item!
                                  (let [contact (-> @reactions/pages :picked-contact)]
                                    (assoc @new-thing :contact contact)))}])}

          [:div {:style {:padding "10px"}}
            [ui/list
              {:style {:padding "0"
                       :margin-left "-10px"
                       :margin-right "-10px"}}
              (let [contact (-> @reactions/pages :picked-contact)]
                [ui/list-item {:primary-text (:name contact)
                               :disabled true
                               :right-icon-button (r/as-element [ui/icon-button
                                                                  {:on-click #(dispatch! [:start-contact-picker])}
                                                                  [ic/editor-mode-edit]])
                               :left-avatar (avatar contact)}])]
            [ui/auto-complete
              {:dataSource @reactions/deduped-items
               :full-width true
               :searchText (-> @reactions/pages :param)
               :floatingLabelText "Item"
               :error-text (:item-error @new-thing)
               :onUpdateInput #(item-update-new-thing new-thing %1 %2)}]
            [text-field {:field :quantity
                         :atom new-thing
                         :label "Quantity"
                         :type "number"
                         :validator (partial validator "This fields should be at least 1" #(<= % 0))
                         :value "1"}]]])))


(defn get-page
  "Gets a page by name"
  [page-name]
  (case page-name
    :home [home]
    :new  [new-item]
    :details [details]
    :details-by-item [details-by-item]
    [home]))
