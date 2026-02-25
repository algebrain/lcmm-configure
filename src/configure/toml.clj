(ns configure.toml
  (:require [clojure.string :as str]
            [configure.keys :as keys])
  (:import [org.tomlj Toml TomlTable]))

(defn- table->map
  "Converts TomlTable to a Clojure map, recursively."
  [^TomlTable t]
  (into {}
        (map (fn [k]
               (let [v (.get t k)]
                 (cond
                   (instance? TomlTable v) [k (table->map v)]
                   :else [k v]))))
        (.keySet t)))

(defn read-toml-file
  "Reads TOML file and returns a flat map with dot-keys."
  [path]
  (let [result (Toml/parse (java.nio.file.Paths/get path (make-array String 0)))]
    (when (.hasErrors result)
      (throw (ex-info "TOML parse error" {:errors (str/join "; " (.getErrors result))})))
    (keys/flatten-map (table->map result))))
