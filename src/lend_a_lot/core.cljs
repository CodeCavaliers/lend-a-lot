(ns lend-a-lot.core
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [lend-a-lot.navigation :as nav]
            [lend-a-lot.pages :as pages]
            [lend-a-lot.store :as store]
            [lend-a-lot.db :as db]
            [lend-a-lot.theme :refer [theme]]
            [clojure.core.async :refer [<! chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(.. js/document (addEventListener "deviceready" db/bootstrap-database))

(defn app [conn]
  (let [page (-> @store/state :pages :current-page)]
    [ui/mui-theme-provider
      {:mui-theme theme}
      (pages/get-page page)]))

(let [contacts-promise (db/all-contacts)
      items-promise    (db/all-lent-items)]
  (go (let [contacts (<! contacts-promise)
            items    (<! items-promise)]
        (store/dispatch! [:load-data contacts items]))))

(r/render [app]
  (js/document.getElementById "app"))
