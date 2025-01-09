(defproject orgroam-to-obsidian "0.1.0"
  :description "Converter from org-roam files to obsidian markdown files"
  :license {:name "bsd-3-clause"
            :url "https://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.seancorfield/honeysql "2.4.1026"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.41.2.1"]]
  :main orgroam-to-obsidian.core
  :repl-options {:init-ns orgroam-to-obsidian.core})
