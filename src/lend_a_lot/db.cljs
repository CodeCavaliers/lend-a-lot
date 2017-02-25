(ns lend-a-lot.db
  (:require [datascript.core :as d]))

(def schema {:item/user {:db/valueType :db.type/ref}})

(def conn (d/create-conn schema))



(defn populate-with-mock [conn]
  (d/transact!
    conn
    [{:db/id -1
      :user/name "Paul"}
     {:db/id -2
      :user/name "Nicu"}
     {:db/id -3
      :item/name "Tigari"
      :item/quantity 3
      :item/user -1}
     {:db/id -5
      :item/name "Cablu Telefon"
      :item/quantity 1
      :item/user -1}
     {:db/id -4
      :item/name "Tigari"
      :item/quantity 2
      :item/user -2}]))



(defn all-users [conn]
  (posh.reagent/q
       '[:find ?id ?user ?item ?item-name ?quantity
         :where [?id :user/name ?user]
                [?item :item/user ?id]
                [?item :item/name ?item-name]
                [?item :item/quantity ?quantity]]
   conn))

(defn users-by-name [db name]
  (d/q '[:find ?u
          :in $ ?name
          :where [?u :user/name ?name]]
    db
    name))

(defn add-quantity-to-item [conn [item current-quantity] quantity]
  (let [new-quantity (+ (int current-quantity) (int quantity))]
    (d/transact! conn [{:db/id item :item/quantity new-quantity}])))

(defn insert-new-item [conn user item quantity]
  (d/transact! conn [{:db/id -1
                      :item/name item
                      :item/quantity quantity
                      :item/user user}]))


(defn add-to-user [conn user-id item quantity]
  (let [items (d/q '[:find ?item-id ?quantity
                     :in $ ?user-id ?item
                     :where [?item-id :item/user ?user-id]
                            [?item-id :item/name ?item]
                            [?item-id :item/quantity ?quantity]]
                @conn user-id item)
         found? (not (empty? items))]
    (println user-id)
    (if found?
      (add-quantity-to-item conn (first items) quantity)
      (insert-new-item conn user-id item quantity))))



(defn new-user-with-item [conn name item quantity]
  (d/transact! conn [{:db/id -1 :user/name name}
                     {:db/id -2 :item/name item :item/quantity quantity
                      :item/user -1}]))

(defn save-new-thing [conn new-thing]
  (let [name (:name new-thing)
        item (:item new-thing)
        quantity (:quantity new-thing)]
    (when (and name item (> quantity 0))
      (let [users-by-name (users-by-name @conn name)
            found? (not (empty? users-by-name))]
        (if found?
          (add-to-user conn (ffirst users-by-name) item quantity)
          (new-user-with-item conn name item quantity))))))
