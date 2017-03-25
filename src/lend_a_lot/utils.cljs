(ns lend-a-lot.utils)


(defn index-by [f xs]
 (reduce
   (fn [index x]
     (assoc index (f x) x))
   {}
   xs))


(defn extract-photo [contact]
   (let [photo-array (or (aget contact "photos") [])
         photo (or (first photo-array) {})]
     (or (aget photo "value") nil)))

(defn create-contact [contact]
   {:id (aget contact "id") :name (aget contact "displayName")
    :photo-url (extract-photo contact)})
