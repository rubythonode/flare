(ns flare.embeddings
  (:require [flare.computation-graph :as cg]
            [flare.core :as flare]
            [flare.node :as node]))

(defprotocol Embedding
  (lookup [this obj])
  (vocab [this])
  (embedding-size [this]))

(defn sent-nodes
  "take a sentence and return sequence of constant node
   tensors, where each consant has the original word
   as part of the name.

   Will use `unk` if given for unknown tokens, or omit
   if `unk` isn't passed in"
  ([emb sent] (sent-nodes emb sent nil))
  ([emb sent unk]
   (for [word sent
         :let [e (lookup emb word)]
         :when (or e unk)]
     (node/const (node/gen-name "word") (or e unk)))))

(deftype FixedEmbedding [^java.util.Map m ^long emb-size]
  Embedding
  (lookup [this obj] (.get m obj))
  (vocab [this] (seq (.keySet m)))
  (embedding-size [this] emb-size)

  clojure.lang.Seqable
  (seq [this] (map (juxt key val) m)))

(defn fixed-embedding
  [factory emb-size obj-vec-pairs]
  (let [m (java.util.HashMap.)
        expected-shape [emb-size]]
    (doseq [[obj nums] obj-vec-pairs]
      (when-let [dupe (.get m obj)]
        (throw (ex-info "Duplicate entry" {:dupe obj})))
      (let [t (flare/from factory nums)
            s (flare/shape t)]
        (when (not= s expected-shape)
          (throw (ex-info "embedding doesn't have same shape"
                          {:expected [embedding-size] :actual s})))
        (.put m obj t)))
    (FixedEmbedding. m (long emb-size))))

(defn read-text-embedding-pairs [rdr]
  (for [^String line (line-seq rdr)
        :let [fields (.split line " ")]]
    [(aget fields 0)
     (map #(Double/parseDouble ^String %) (rest fields))]))
