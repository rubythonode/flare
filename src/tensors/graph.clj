(ns tensors.graph
  (:require [schema.core :as s]
            [tensors.core :as tensors]
            [clojure.string :as str]
            [plumbing.core :as p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Graph Walks

(defn bottom-up-walk [node walk-fn]
  ;; must `doall` for children since `walk-fn` can have side-effects
  (if-let [cs (seq (:children node))]
    (walk-fn (assoc node :children (mapv #(bottom-up-walk % walk-fn) cs)))
    (walk-fn node)))

(defn top-down-walk [node walk-fn]
  ;; walk-fn can update children so do a let-binding
  (let [node (walk-fn node)]
    (if-let [cs (seq (:children node))]
      (assoc node :children (mapv #(top-down-walk % walk-fn) cs))
      node)))

(defn post-order-nodes [target]
  (conj (vec (mapcat post-order-nodes (:children target))) target))

