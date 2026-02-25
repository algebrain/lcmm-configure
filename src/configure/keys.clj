(ns configure.keys
  (:require [clojure.string :as str]))

(def ^:private default-secret-tokens
  #{"password" "secret" "token" "apikey" "api_key"})

(defn- key->string
  [k]
  (cond
    (keyword? k) (name k)
    (symbol? k) (name k)
    :else (str k)))

(defn- default-secret-key?
  [k]
  (let [segments (->> (str/split (str/lower-case (key->string k)) #"\.")
                      (remove str/blank?))]
    (some default-secret-tokens segments)))

(defn normalize-key
  "Normalize a key to a lower-case string."
  [k]
  (-> (name k) str/lower-case))

(defn module-prefix
  "Builds an ENV prefix for a module name, e.g. users -> USERS__."
  [module-name]
  (let [base (-> module-name
                 (str/replace #"[^A-Za-z0-9]" "_")
                 str/upper-case)]
    (str base "__")))

(defn env->key
  "Maps an ENV variable name to a dot key. Returns nil if prefix doesn't match."
  [module-name env-key]
  (let [prefix (module-prefix module-name)]
    (when (str/starts-with? env-key prefix)
      (let [tail (subs env-key (count prefix))
            parts (str/split tail #"__")]
        (->> parts
             (map str/lower-case)
             (str/join "."))))))

(defn flatten-map
  "Flattens a nested map into dot-keys. Example: {:db {:url x}} -> {db.url x}."
  ([m] (flatten-map nil m))
  ([prefix m]
   (reduce-kv
    (fn [acc k v]
      (let [k' (normalize-key k)
            key (if prefix (str prefix "." k') k')]
        (if (map? v)
          (merge acc (flatten-map key v))
          (assoc acc key v))))
    {}
    m)))

(defn mask-secrets
  "Mask values for keys that look like secrets."
  ([config]
   (mask-secrets config {}))
  ([config {:keys [secret-key?]}]
   (let [secret-key? (or secret-key? default-secret-key?)]
     (reduce-kv
      (fn [acc k v]
        (if (secret-key? k)
          (assoc acc k "***")
          (assoc acc k v)))
      {}
      config))))
