(ns lend-a-lot.core
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [re-frame.core :as re]
            [lend-a-lot.navigation :as nav]
            [lend-a-lot.pages :as pages]
            [lend-a-lot.theme :refer [theme]]))

(enable-console-print!)

(defn get-page [{page :page param :param}]
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
