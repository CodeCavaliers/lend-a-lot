(ns lend-a-lot.utils)


(defn index-by [f xs]
 (reduce
   (fn [index x]
     (assoc index (f x) x))
   {}
   xs))
