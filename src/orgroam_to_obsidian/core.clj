(ns orgroam-to-obsidian.core
  (:require [clojure.java.jdbc :as j]
            [honey.sql :as sql]
            [clojure.string :as str]
            [clojure.java.shell :as sh]))

(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "./input/org-roam.db"})

(def dbresults (j/query db (sql/format {:select [:*], :from [:nodes], :order-by [:id]})))

(defn sanitise-row [row]
  (into {}
        (for [[k v] row]
          [(keyword k)
           (if (string? v) (str/replace v "\"" "") v)])))

(defn sanitise-results [results]
  (into []
        (for [row results]
          (sanitise-row row))))

(defn org->gfm [s]
  (:out (sh/sh "pandoc" "--wrap=none" "-t" "gfm" "-f" "org" :in s)))

(defn import-row-contents [row]
  (merge row {:content (-> (:file row)
                           (str/replace #".*roam/(.*)" "./input/org-roam/$1")
                           slurp
                           org->gfm)}))

(defn import-contents [results]
  (into []
        (for [row results]
          (do (prn (:title row))
              (import-row-contents row)))))

(defn import-files [results]
  (-> results
      sanitise-results
      import-contents))

(defn list-titles [results]
  (into []
        (for [row results]
          {:title (:title row), :id (:id row)})))

(defn target-title [target titles]
  (let [id (subs target 3)]
    (-> (filter (fn [x] (= id (:id x))) titles)
        first
        :title)))

(defn transform-links [content matches titles]
  (if-not (empty? content)
    (if-not (empty? matches)
      (let [[_ full _ target] (first matches)]
        (let [tt (target-title target titles)]
          (-> (if-not (empty? tt)
                (if (str/starts-with? target "id:")
                  (str/replace content full (str "[[" tt "]]")))
                content)
              (transform-links (rest matches) titles))))
      content)))

(defn transform-result-links
  ([results]
   (let [titles (list-titles results)]
     (transform-result-links results titles)))
  ([results titles]
   (into []
         (for [row results]
           (let [matches (re-seq #"(\[([^\]]+)\]\(([^)]+)\))" (:content row))]
             (merge row {:content (transform-links (:content row) matches titles)}))))))

(defn md-filename [title]
  (-> (str/replace title #"[\x00\/\\:\*\?\"<>\|]" "-")
      (str ".md")))

(defn write-files [results]
  (doseq [row results]
    (let [filename (md-filename (:title row))]
      (spit (str "./output/" filename) (:content row)))))

(defn run []
  (-> dbresults
      import-files
      transform-result-links
      write-files))

(defn -main [& _]
  (run)
  (shutdown-agents))
