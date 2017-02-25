(ns lend-a-lot.core
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [re-frame.core :as re]
            [posh.reagent :as p]
            [lend-a-lot.navigation :as nav]
            [lend-a-lot.pages :as pages]
            [lend-a-lot.theme :refer [theme]]
            [lend-a-lot.db :as db]))

(enable-console-print!)


(defn get-page [{page :page param :param} conn]
  (case page
    :home [pages/home conn]
    :new [pages/new-item conn]
    :details [pages/details conn param]
    [pages/home conn]))

(defn app [conn]
  [ui/mui-theme-provider
    {:mui-theme theme}
    (let [page (re/subscribe [:pages])]
      (get-page @page conn))])




(defn ^:export main []
  (db/populate-with-mock db/conn)          ;; mock data
  (p/posh! db/conn)
  (r/render [app db/conn]
          (js/document.getElementById "app")))


(defonce init (main))

(defn on-js-reload [])
