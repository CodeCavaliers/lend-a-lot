(ns lend-a-lot.theme
  (:require [cljs-react-material-ui.core :refer [get-mui-theme]]))

(defn color [name]
  (aget (aget js/MaterialUIStyles "colors") name))

(def palette
  { :primary1Color (color "red500")
    :primary3Color (color "grey400")
    :primary2Color (color "red500")
    :accent1Color (color "pinkA200")
    :accent2Color (color "grey100")
    :accent3Color (color "grey500")
    :textColor (color "darkBlack")
    :alternateTextColor (color "white")
    :canvasColor (color "white")
    :borderColor (color "grey300")
    :pickerHeaderColor (color "red500")
    :shadowColor (color "fullBlack")})

(def theme
  (get-mui-theme
    {:palette palette}))
