(ns lend-a-lot.effects
  "lend-a-lot.effects registers all effect handlers and all effects
  for lend-a-lot.
  All types of effect a groups together in sections."
  (:require [lend-a-lot.effect-processor :as e]
            [clojure.core.async :refer [<! put! promise-chan]]
            [lend-a-lot.db :as db]
            [lend-a-lot.utils :as utils :refer [index-by]]
            [clojure.spec :as s])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(e/register-effect-handler :db
  (fn [ctx effect-result]
    (swap! ctx assoc :db effect-result)))

(s/def :nav/to
  (s/cat :action #{:to}
         :location string?))

(s/def :nav/back
  (s/cat :action #{:back}))

(s/def :nav/action
  (s/or :go-to :nav/to
        :go-back :nav/back))

(e/register-effect-handler :nav
    (fn [ctx effect]
      (let [e (s/conform :nav/action effect)]
        (if (= e ::s/invalid)
          (throw (js/Error. (s/explain-str :nav/action effect)))
          (case (first e)
                :go-to (set! js/window.location.href (:location (second e)))
                :go-back  (. js/window.history back))))))


(e/register-effect-handler :async
  (fn [ctx effect-result]
    (go (let [next (<! effect-result)]
          (e/dispatch! next)))))

(e/register-effect-handler :async-stream
  (fn [_ results]
    (go (let [results (<! results)]
          (doseq [r results]
            (e/dispatch! r))))))

;; ===== Initialziation ======

(def xid (map :id))
(def xuserId (map :userId))

(defn db->data [contacts items]
  (let [item-ids (into #{} xid items)
        user-ids (into #{} xuserId items)
        contacts (filter (fn [c] (user-ids (str (:id c)))) contacts)]
    {:users (into #{} xid contacts)
     :items item-ids
     :users-index (index-by :id contacts)
     :items-index (index-by :id items)
     :user->items (->> items
                      (group-by :userId)
                      (reduce-kv (fn [acc k v] (assoc acc k (into #{} xid v))) {}))}))


(defn db->settings [raw-settings]
  (let [settings-by-name (index-by :name raw-settings)
        group-by-user-flag (= "true" (:value (get settings-by-name "group-by-user")))]
    {:group-by-user group-by-user-flag}))


(e/register-effect :init
  (fn [{db :db} _]
    {:db {:pages {:current-page :home}
          :loaded false
          :drawer-open false
          :settings {:group-by-user true}
          :list-filter ""
          :data { :users #{}
                  :items #{}
                  :user->items {}
                  :users-index {}
                  :items-index {}}}

     :async (go [:data-loaded (<! (db/all-contacts))
                              (<! (db/all-lent-items))
                              (<! (db/all-settings))])}))

(e/register-effect :data-loaded
  (fn [{db :db} [_ contacts items settings]]
    {:db (-> db
            (assoc :loaded true)
            (assoc :settings (db->settings settings))
            (assoc :data (db->data contacts items)))}))


;========= Settings =========

(e/register-effect :settings/group-by-user
  (fn [{db :db} [_ type]]
    {:db (assoc-in db [:settings :group-by-user] type)
     :async (db/store-setting "group-by-user" (str type))}))

(e/register-effect :drawer
  (fn [{db :db} [_ state]]
    {:db (assoc db :drawer-open state)}))

;; ======== Pages =========
(defn clear-params [state page]
  (if (not= :new page)
    (-> state
      (update :pages dissoc :picked-contact))
    state))

(e/register-effect :pages
  (fn [{db :db} [_ page param]]
    {:db (-> db
            (assoc-in [:pages :current-page] page)
            (assoc-in [:pages :param] param)
            (clear-params page))}))

(e/register-effect :picked-contact
  (fn [{db :db} [_ contact]]
    {:db (assoc-in db [:pages :picked-contact] contact)}))


;; ======= Item manip ======

(defn set-conj
  "Nillable conj for sets"
  [set x]
  (if (nil? set)
    #{x}
    (conj set x)))

(defn add-new-item [data item]
  (let [{:keys [userId id userName photo-url]} item]
    (-> data
      (update-in [:users] conj userId)
      (update-in [:items] conj id)
      (assoc-in [:users-index userId] {:id userId :name userName :photo-url photo-url})
      (assoc-in [:items-index id] (dissoc item :userName))
      (update-in [:user->items userId] set-conj id))))

(e/register-effect :new-item
  (fn [{db :db} [_ item]]
    {:db (update db :data add-new-item item)}))

(e/register-effect :update-item
  (fn [{db :db} [_ item-id item-quantity]]
    {:db (assoc-in db [:data :items-index item-id :quantity] item-quantity)}))


;;====== Navigation =======


(e/register-effect :nav-to
  (fn [_ [_ location]]
    {:nav [:to location]}))

(e/register-effect :nav-back
  (fn [_ _]
    {:nav [:back]}))


;;====== Contacts ======

(defn pick-contact [item]
  (let [result (promise-chan)]
    (go (let [contacts (<! db/contacts)
              pickContactFn (aget contacts "pickContact")]
          (pickContactFn
            (fn [c]
              (let [contact (utils/create-contact c)]
                (put! result
                   [[:picked-contact contact]
                    [:nav-to (str "#/new?item=" item)]]))))))

    result))

(e/register-effect :start-contact-picker
  (fn [{db :db} _]
    {:async-stream (pick-contact "")}))

(e/register-effect :add-to-existing-user
  (fn [{db :db} [_ user]]
    {:db (assoc-in db [:pages :picked-contact] user)
     :nav [:to "#/new"]}))

(e/register-effect :add-user-to-item
  (fn [{db :db} [_ item]]
    {:async-stream (pick-contact (:item-name item))}))

(defn update-item-quantity [item-id quantity]
  (go (<! (db/update-item-quantity item-id quantity))
      [:update-item item-id quantity]))

(e/register-effect :update-item-quantity
  (fn [_ [_ item-id quantity]]
    {:async (update-item-quantity item-id quantity)}))


(e/register-effect :save-new-item
  (fn [_ [_ id name item quantity photo-url]]
    {:async-stream
      (go (let [result (<! (db/save-new-item! id item quantity))]
            [[:new-item (-> result (assoc :photo-url photo-url) (assoc :userName name))]
             [:nav-back]]))}))

(e/register-effect :list-filter
  (fn [{db :db} [_ filter]]
    {:db (assoc db :list-filter filter)}))
