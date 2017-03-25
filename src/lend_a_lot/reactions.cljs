(ns lend-a-lot.reactions
  "reactions contains all queries executed the view layer.
  Think 'materializes views', cause that's basically it."
  (:require [reagent.ratom :refer [reaction]]
            [lend-a-lot.utils :refer [index-by]]
            [lend-a-lot.effect-processor :as e]))


;; ======= Query helper functions =======

(defn user-by-id
  "Get a user by id"
  [state id]
  (let [users (-> state :data :users-index)
        user (users id)]
    (assoc user :id id)))

(defn item-by-id
  "Get an item by id"
  [state id]
  (let [items (-> state :data :items-index)
        item  (items id)]
    (assoc item :id id)))

(defn user-with-items-by-id
  "User + all items related to user by user-id"
  [state id]
  (let [user (user-by-id state id)
        items-for-user (-> state :data :user->items (get id))
        full-items (map (partial item-by-id state) items-for-user)
        without-0-quantity (filter #(not= (:quantity %) 0) full-items)]
    (assoc user :items without-0-quantity)))


(defn all-users
  "Get all users"
  [state]
  (let [users (-> state :data :users)]
    (->> users
      (map (partial user-with-items-by-id state))
      (filter #(not= (count (:items %)) 0)))))

(defn all-items
  "Get all items"
  [state]
  (let [item-ids (-> state :data :items)
        items (->> item-ids (map (partial item-by-id state)))]
    items))



(defn item->user-quantity [state item]
  (let [user (user-by-id state (:userId item))]
    {:name (:name item)
     :user-name (:name user)
     :id (:id item)
     :userId (:userId item)
     :gen-id (str (:id item) "-" (:userId item))
     :quantity (:quantity item)}))

(defn items-by-name [groups-by-item-name]
  (let [item-name (first groups-by-item-name)
        users     (second groups-by-item-name)
        id        (str (clojure.string/join "-" (map :id users) ) ","
                       (clojure.string/join "-" (map :userId users)))]
    {:item-name item-name
     :id id
     :users users}))

(defn users-home-page
  "Query to populate the home page"
  [state]
  (all-users state))



(defn items-home-page
  [state]
  (let [items (all-items state)
        users-by-items (->> items
                          (map (partial item->user-quantity state))
                          (filter #(> (:quantity %) 0)))
        by-item-name (group-by :name users-by-items)
        data (map items-by-name by-item-name)]
    (->> data
      (sort-by :item-name))))

(defn details
  "Query to populate details page"
  [state]
  (user-with-items-by-id state (-> state :pages :param)))

(defn deduped-items-query [state]
  (->> (all-items state)
      (sort-by :quantity)
      (map :name)
      (distinct)))

(defn item-with-users-by-item-name [items-list item-name]
  (let [items-indexed-by-name (->> items-list
                                (index-by :item-name))]
    (get items-indexed-by-name item-name)))

(defn item-details-query [items-list pages]
  (let [item-name (-> pages :param)]
    (item-with-users-by-item-name items-list item-name)))

;; ========= Queries ===========

(def db (e/reaction-for [:db]))
(def pages (reaction (:pages @db)))
(def settings (reaction (:settings @db)))
(def home-page-by-users (reaction (users-home-page @db)))
(def home-page-by-items (reaction (items-home-page @db)))
(def user-details (reaction (details @db)))
(def item-details (reaction (item-details-query @home-page-by-items @pages)))
(def deduped-items (reaction (deduped-items-query @db)))

(do @user-details)
