(ns lend-a-lot.db)


(def db (atom))

(.. js/window
  (addEventListener "deviceready"
    (fn []
      (reset! db (.. js/window -sqlitePlugin)))))

(defn save-new-item! [user-id item quantity])

(println (.. js/window -sqlitePlugin))
