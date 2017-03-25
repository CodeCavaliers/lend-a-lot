(ns lend-a-lot.effect-processor
  "Effect processor 'framework'.
  Here are the internals of storing app state and dispatching actions."
  (:require [clojure.spec :as s]
            [clojure.core.async :refer [<! put! chan]]
            [reagent.core :refer [atom]]
            [reagent.ratom :refer [reaction]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(s/def ::action (s/cat :type keyword?
                       :payload (s/* any?)))

(s/def ::effect (s/map-of keyword? any?))

(s/def ::effect-registry (s/map-of keyword? ::effect))

(def ^:private context
  "Context the whole application.
  This is where all effects effect handler and app state are stored"
  (atom {}))

(def ^:private action-channel (chan))

(defn register-effect
  "Registers a function to process a action into an effect.

   Effects are data descriptions of what should happen in the application.
   Effects are passed to their respective effect handlers during the effect processing phase."
  [action-name handler]
  (swap! context assoc-in [:effect-registry action-name] handler)
  nil)

(s/fdef register-effect
  :args (s/cat :action-name keyword?
               :effect-handler fn?)
  :ret nil?)


(defn register-effect-handler
  "Registers a function to process effects of a given type.

   Effect processors are invoked with the coresponing effects during the effect
   processing phase."
  [handler-name handler-function]
  (swap! context assoc-in [:effect-handler-registry handler-name] handler-function)
  nil)

(s/fdef register-effect-handler
  :args (s/cat :handler-name keyword?
               :handler-fn fn?)
  :ret nil?)


(defn- process-effect
  "The effect procesing step"
  [effect action]
  (let [ctx @context
        effect-result (effect ctx action)]
    (doseq [[effect-type effect-data] effect-result]
      (let [processor (get-in ctx [:effect-handler-registry effect-type])]
        (processor context effect-data)))))

(s/fdef process-effect
  :args (s/cat :effect ::effect
               :action ::action)
  :ret nil?)


(defn- handle-action
  "Determines the correct effect handler for a given action and
  starts the effect processing phase with given effect handler and action"
  [effects action]
  (let [effect (effects (first action))]
    (if-not effect
      (throw (js/Error. (str "No effect handler registred for action " type)))
      (process-effect effect action))))

(s/fdef handle-action
  :args (s/cat :registry ::effect-registry
               :action ::action)
  :ret nil?)

(go-loop []
  (let [action (<! action-channel)
        effect-registry (get-in @context [:effect-registry])]
    (handle-action effect-registry action)
    (recur)))

(defn dispatch!
  "Triggers an action in the system.
  Validates that actions are of correct form.
  Starts the effect resultion process based on given action.

  Action must be a vector of this format:
    [:action-name & payload]"
  [action]
  (let [valid-action (s/conform ::action action)]
    (if (= valid-action ::s/invalid)
      (throw (js/Error. (s/explain-str ::action action)))
      (do (put! action-channel action) nil))))


(s/fdef dispatch!
  :args (s/cat :action ::action)
  :ret nil?)


(defn reaction-for [path]
  (reaction (get-in @context path)))
