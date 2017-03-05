(ns lend-a-lot.core
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [lend-a-lot.navigation :as nav]
            [lend-a-lot.pages :as pages]
            [lend-a-lot.store :as store]
            [lend-a-lot.theme :refer [theme]]))

(enable-console-print!)


(defn app [conn]
  (let [pages @store/pages-state]
    [ui/mui-theme-provider
      {:mui-theme theme}
      (pages/get-page (:current-page pages))]))


(r/render [app]
  (js/document.getElementById "app"))


(defn on-js-reload [])
