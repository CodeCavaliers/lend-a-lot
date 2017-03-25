(ns lend-a-lot.db
  "!!!Warning ugly js interop ahead!!!

  This namespace handels interop with cordova-sqlite-plugin and
  cordova-contacts-plugin.

  Important items are
    db                    - a promise-chan containing the sqlite plugin instance
    contacts              - a promise-chan containing the contacts plugin instance
    all-contacts          - a query for all contacts using the contacts api
    bootstrap-database    - callback that sets up this whole namespace
                            used in core as a callback to 'deviceready' cordova event
    save-new-item!        - saves a new items or updates the quantity of an item
    update-item-quantity! - updates an item quantity

  So don't bother looking here."

  (:require [clojure.core.async :as async :refer [<! >!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def db (async/promise-chan))
(def contacts (async/promise-chan))

(defn map-results [result]
  (let [getter (.-item (.-rows result))]
    (reduce
       (fn [acc, index]
         (conj acc (js->clj (getter index) :keywordize-keys true)))
       []
       (range (-> result .-rows .-length)))))

(defn transaction! [f]
  (go
    (.transaction (<! db) f)))

(defn execute-sql!
  ([sql] (execute-sql! sql []))
  ([sql vals]
   (let [result (async/promise-chan)]
     (go
       (.executeSql (<! db) sql vals
           #(async/put! result (map-results %))
           #(println "ERROR " %)))
     result)))



(defn item-by-id [item-id]
  (go (first (<! (execute-sql! (str "SELECT * FROM LentItems WHERE id = " item-id))))))

(defn all-lent-items []
  (execute-sql! "SELECT * FROM LentItems"))

(defn map-contacts [contacts]
  (map (fn [contact] {:id (aget contact "id") :name (aget contact "displayName")})
       contacts))

(defn all-contacts []
  (let [result (async/promise-chan)]
    (go
      (.find (<! contacts)
          #js[{}]
          (fn [contacts]
            (async/put! result (map-contacts contacts)))
          (fn [error]
            (println "Error" error))))
    result))

(defn item-with-name-for-user [user-id item-name]
  (execute-sql! (str "SELECT * FROM LentItems WHERE userId=" user-id " and name='" item-name "';")))

(defn bootstrap-database
  "Boostraps sqlite plugin and contacts plugin.
  Called on 'deviceready' callback.

  Stores instances of the database and contactsPlugin in global promise channels."
  []
  (let [sqlitePlugin (aget js/window "sqlitePlugin")
        contactsPlugin (aget (aget js/window "navigator") "contacts")
        database (.openDatabase sqlitePlugin #js{:name "lend-a-lot.db" :location "default"})]
    ; make the db on first boot
    (.transaction
      database
      (fn [tx]
        (.executeSql tx "CREATE TABLE IF NOT EXISTS LentItems (id integer primary key autoincrement unique,
                                                               userId varchar(16),
                                                               name,
                                                               quantity integer)"))
      (fn [error] (println error))
      (fn []
        (async/put! db database)
        (async/put! contacts contactsPlugin)))))


(defn save-new-item!
  "Stores an item based on user-id.
  If item is already associated with a user, quanity gets added.
  If item does not exist for user, a new entry is created for item and user with quantity."
  [user-id item quantity]
  (go
    (let [current-item (first (<! (item-with-name-for-user user-id item)))
          quantity (js/parseInt quantity)]
      (if (nil? current-item)
        (do
          (<! (execute-sql!
                (str "INSERT INTO LentItems (userId, name, quantity) VALUES (" user-id ", '" item "', " quantity ");")))
          (first (<! (item-with-name-for-user user-id item))))
        (do
          (<! (execute-sql!
                (str "UPDATE LentItems SET quantity = " (+ quantity (-> current-item :quantity js/parseInt))
                     " WHERE id = " (:id current-item) ";")))
          (update current-item :quantity + quantity))))))

(defn update-item-quantity [item-id new-quantity]
  (execute-sql!
    (str "UPDATE LentItems SET quantity = "
            new-quantity
          " WHERE id=" item-id ";")))
