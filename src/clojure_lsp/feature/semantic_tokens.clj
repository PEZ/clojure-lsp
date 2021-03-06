(ns clojure-lsp.feature.semantic-tokens
  (:require
    [clojure.string :as string]))

(def token-types
  [:type
   :function
   :macro])

(def token-types-str
  (->> token-types
       (map name)
       vec))

(def token-modifiers
  [])

(def token-modifier -1)

(defn ^:private usage-inside-range?
  [{usage-row :row usage-end-row :end-row}
   {:keys [row end-row]}]
  (and (>= usage-row row)
       (<= usage-end-row end-row)))

(defn ^:private usage->absolute-token
  [{:keys [row col end-col]}
   token-type]
  [(dec row)
   (dec col)
   (- end-col col)
   (.indexOf token-types token-type)
   token-modifier])

(defn ^:private alias-reference-usage->absolute-token
  [{:keys [row col end-col str]}]
  (let [slash-col (string/index-of str "/")
        function-col (+ col (inc slash-col))
        alias-end-col (+ col slash-col)]
    [(usage->absolute-token {:row row :col col :end-col alias-end-col} :type)
     (usage->absolute-token {:row row :col function-col :end-col end-col} :function)]))

(defn ^:private usages->absolute-tokens
  [usages]
  (->> usages
       (sort-by (juxt :row :col))
       (map
         (fn [{:keys [tags] :as usage}]
           (cond
             (contains? tags :macro)
             [(usage->absolute-token usage :macro)]

             (contains? tags :declared)
             [(usage->absolute-token usage :function)]

             (contains? tags :refered)
             [(usage->absolute-token usage :function)]

             (contains? tags :alias-reference)
             (alias-reference-usage->absolute-token usage))))
       (remove nil?)
       (mapcat identity)))

(defn ^:private absolute-token->relative-token
  [tokens
   index
   [row col length token-type token-modifier :as token]]
  (let [[previous-row previous-col _ _ _] (nth tokens (dec index) nil)]
    (cond
      (nil? previous-row)
      token

      (= previous-row row)
      [0
       (- col previous-col)
       length
       token-type
       token-modifier]

      :else
      [(- row previous-row)
       col
       length
       token-type
       token-modifier])))

(defn full-tokens
  [usages]
  (let [absolute-tokens (usages->absolute-tokens usages)]
    (->> absolute-tokens
         (map-indexed (partial absolute-token->relative-token absolute-tokens))
         flatten)))

(defn range-tokens
  [usages range]
  (let [range-usages (filter #(usage-inside-range? % range) usages)
        absolute-tokens (usages->absolute-tokens range-usages)]
    (->> absolute-tokens
         (map-indexed (partial absolute-token->relative-token absolute-tokens))
         flatten)))
