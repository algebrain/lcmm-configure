(ns configure.env
  (:require [clojure.string :as str]
            [configure.keys :as keys]))

(defn env->config
  "Filters ENV by module prefix and maps to dot-keys. Empty values are ignored."
  [module-name env]
  (reduce-kv
   (fn [acc k v]
     (if-let [dot-key (keys/env->key module-name k)]
       (let [val (if (string? v) (str/trim v) v)]
         (if (or (nil? val) (and (string? val) (str/blank? val)))
           acc
           (assoc acc dot-key val)))
       acc))
   {}
   env))
