(ns lend-a-lot.core
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [lend-a-lot.navigation :as nav]
            [lend-a-lot.pages :as pages]
            [lend-a-lot.reactions :as reactions]
            [lend-a-lot.db :as db]
            [lend-a-lot.theme :refer [theme]]
            [clojure.core.async :refer [<! chan]]
            [clojure.spec :as s]
            [lend-a-lot.effect-processor :as e]
            [lend-a-lot.effects])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(.. js/document (addEventListener "deviceready" db/bootstrap-database))

(defn app [conn]
  (let [page (-> @reactions/pages :current-page)]
    [ui/mui-theme-provider
      {:mui-theme theme}
      (pages/get-page page)]))

(e/dispatch! [:init])

(r/render [app]
  (js/document.getElementById "app"))
