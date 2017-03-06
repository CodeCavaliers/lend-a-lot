(ns lend-a-lot.store
  (:require [reagent.core :as r]))


(defonce state
  (r/atom {:counter 0
           :pages {:current-page :home}
           :data { :users ["1" "2"]
                   :items ["3" "4"]
                   :entities {"1" {:name "Paul" :items ["3" "5"]}
                              "2" {:name "Nicu" :items ["4"]}
                              "3" {:name "Tigari" :quantity 2}
                              "5" {:name "Cablu Laptop" :quantity 1}
                              "4" {:name "Tigari" :quantity 3}}}}))


(def pages-state (r/cursor state [:pages]))

(defn comp-reducers-2 [a b]
  (fn [state action]
    (a (b state action) action)))

(defn comp-reducers [& reducers]
  (reduce comp-reducers-2 identity reducers))

(defn for-path [path reducer]
  (fn [state action]
    (update-in state path reducer action)))

(defn clear-params [pages page]
  (if (not= :new page)
    (-> pages
      (dissoc :picked-contact))
    pages))


(def pages-reducer
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


(defn add-new-item [data params]
  (println params)
  data)

(def data-reducer
  (for-path [:data]
    (fn [data [type params]]
      (case type
        :new-item (add-new-item data params)
        data))))


(def reducer
  (comp-reducers pages-reducer
                 data-reducer))


; ============ Queries =============

(defn entity-by-id [state id]
  (let [entities (-> state :data :entities)
        entity (entities id)]
    (assoc entity :id id)))

(defn user-by-id [state id]
  (let [user (entity-by-id state id)]
    (update user :items #(map (partial entity-by-id state) %))))

(defn home-page [state]
  (let [users (-> state :data :users)]
    (map (partial user-by-id state) users)))

(defn all-users [state]
  (let [users (-> state :data :users)]
    (map (partial entity-by-id state) users)))

(defn all-items [state]
  (let [item-ids (-> state :data :items)
        items (->> item-ids (map (partial entity-by-id state)))]
    items))

(defn dispatch! [action]
  (swap! state reducer action))
