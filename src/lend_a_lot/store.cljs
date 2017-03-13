(ns lend-a-lot.store  (:require [reagent.core :as r]))


(def state
  (r/atom {:pages {:current-page :home}
           :loading true
           :data { :users #{}
                   :items #{}
                   :user->items {}
                   :users-index {}
                   :items-index {}}}))



; ===== Utils ======

(defn index-by [f xs]
 (reduce
   (fn [index x]
     (assoc index (f x) x))
   {}
   xs))

(def xid (map :id))

(defn db->data [contacts items]
  (let [item-ids (into #{} xid items)
        user-ids (into #{} (map :userId) items)
        contacts (filter (fn [c] (user-ids (str (:id c)))) contacts)]
    {:users (into #{} xid contacts)
     :items item-ids
     :users-index (index-by :id contacts)
     :items-index (index-by :id items)
     :user->items (->> items
                      (group-by :userId)
                      (reduce-kv (fn [acc k v] (assoc acc k (into #{} xid v))) {}))}))


; ============ Queries =============

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
        items-for-user (-> state :data :user->items (get id))]
    (assoc user :items (map (partial item-by-id state) items-for-user))))


(defn all-users
  "Get all users"
  [state]
  (let [users (-> state :data :users)]
    (map (partial user-with-items-by-id state) users)))

(defn all-items
  "Get all items"
  [state]
  (let [item-ids (-> state :data :items)
        items (->> item-ids (map (partial item-by-id state)))]
    items))

(defn home-page
  "Query to populate the home page"
  [state]
  (all-users state))

(defn comp-reducers-2
  "Compose for two reducers
   Params:
    a - reducer
    b - reducer
    result -> a reducer equivalent to calling (a (b state action) action)"
  [a b]
  (fn [state action]
    (a (b state action) action)))

(defn comp-reducers
  "Compose 'n' reducers with comp-reducers-2"
  [& reducers]
  (reduce comp-reducers-2 identity reducers))

(defn for-path
  "Creates a reducer for a path"
  [path reducer]
  (fn [state action]
    (update-in state path reducer action)))

(defn clear-params [pages page]
  (if (not= :new page)
    (-> pages
      (dissoc :picked-contact))
    pages))


(def pages-reducer
  "Reducer for :pages part of state.
  Handles page transitions and contact picker iternactions."
  (for-path [:pages]
    (fn [state [type page param]]
      (case type
        :pages (-> state
                (assoc :current-page page)
                (assoc :param param)
                (clear-params page))
        :picked-contact (-> state
                          (assoc :picked-contact page))
        state))))

(defn global-reducer
  "Global reducer globaly relevant actions."
  [state [type & params]]
  (case type
    :load-data (assoc state :loading false)
    state))

(defn set-conj
  "Nillable conj for sets"
  [set x]
  (if (nil? set)
    #{x}
    (conj set x)))

(defn add-new-item [data item]
  (let [{:keys [userId id userName]} item]
    (-> data
      (update-in [:users] conj userId)
      (update-in [:items] conj id)
      (assoc-in [:users-index userId] {:id userId :name userName})
      (assoc-in [:items-index id] (dissoc item :userName))
      (update-in [:user->items userId] set-conj id))))


(def data-reducer
  "Reducer for :data. Handels are events relevant to the :data part of state"
  (for-path [:data]
    (fn [data [type & params]]
      (case type
        :load-data (let [contacts (first params)
                         items    (second params)]
                      (db->data contacts items))
        :new-item (add-new-item data (first params))
        data))))


(def reducer
  "The app reducer. Handles all state transitions.
  Created by composition of multiple path reducers."
  (comp-reducers pages-reducer
                 data-reducer
                 global-reducer))


(defn dispatch!
  "Dispatches an action. Applies reducer and action to old state to create new state"
  [action]
  (swap! state reducer action))
