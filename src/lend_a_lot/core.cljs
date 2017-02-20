(ns lend-a-lot.core
  (:require [reagent.core :as r]
            [lend-a-lot.navgation]
            [lend-a-lot.pages :as pages]
            [lend-a-lot.theme :refer [theme]]
            [cljs-react-material-ui.reagent :as ui]
            [re-frame.core :as re]))


(enable-console-print!)

(defn get-page [{page :page param :param}]
  (println page param)
  (case page
    :home [pages/home]
    :details [pages/details param]
    :new [pages/details param]
    [pages/home]))

(defn app []
  [ui/mui-theme-provider
    {:mui-theme theme}
    (let [page (re/subscribe [:pages])]
      (get-page @page))])

(r/render-component [app]
  (.getElementById js/document "app"))

(defn on-js-reload [])
