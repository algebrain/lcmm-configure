(ns configure.core
  (:require [clojure.string :as str]
            [configure.env :as env]
            [configure.keys :as keys]
            [configure.toml :as toml]))

(defn- file-exists?
  [path]
  (when path
    (.exists (java.io.File. path))))

(defn- relative-path?
  [path]
  (when path
    (not (.isAbsolute (java.io.File. path)))))

(defn select-config-file
  "Selects a config file according to rules.
   Returns {:path <path or nil> :source <keyword>}.
   Sources: :explicit, :module-default, :global-default, :none."
  [{:keys [config module-name allow-relative? cwd env-only?]}]
  (let [allow-relative? (if (nil? allow-relative?) true allow-relative?)
        cwd (or cwd ".")]
    (cond
      config
      (do
        (when (and (relative-path? config) (not allow-relative?))
          (throw (ex-info "Relative config paths are not allowed in this mode." {:config config})))
        (if (file-exists? config)
          {:path config :source :explicit}
          (throw (ex-info "Config file not found." {:config config}))))

      (and (not env-only?) allow-relative? module-name)
      (let [module-file (str (java.io.File. cwd (str module-name "_config.toml")))
            global-file (str (java.io.File. cwd "config.toml"))]
        (cond
          (file-exists? module-file) {:path module-file :source :module-default}
          (file-exists? global-file) {:path global-file :source :global-default}
          :else {:path nil :source :none}))

      :else
      {:path nil :source :none})))

(defn- parse-bool
  [s]
  (case (str/lower-case s)
    "true" true
    "false" false
    (throw (ex-info "Invalid boolean value" {:value s}))))

(defn- parse-int
  [s]
  (try
    (Long/parseLong s)
    (catch Exception _
      (throw (ex-info "Invalid integer value" {:value s})))))

(defn- parse-csv
  [s]
  (->> (str/split s #",")
       (map str/trim)
       (remove str/blank?)
       vec))

(defn apply-types
  "Applies optional type parsing for env string values.
   types map example: {db.port :int, debug :bool, allowed :csv}."
  [config types]
  (if (seq types)
    (reduce-kv
     (fn [acc k t]
       (if-let [v (get acc k)]
         (if (string? v)
           (assoc acc k
                  (case t
                    :int (parse-int v)
                    :bool (parse-bool v)
                    :csv (parse-csv v)
                    v))
           acc)
         acc))
     config
     types)
    config))

(defn validate-required
  "Ensures required keys exist and are non-blank."
  [config required]
  (let [missing (->> required
                     (filter (fn [k]
                               (let [v (get config k)]
                                 (or (nil? v)
                                     (and (string? v) (str/blank? v))))))
                     vec)]
    (when (seq missing)
      (throw (ex-info "Missing required config keys" {:missing missing})))
    config))

(defn load-config
  "Loads and merges config for a module.
   opts:
   - :module-name (string, required for env mapping)
   - :config (explicit path)
   - :allow-relative? (default true)
   - :cwd (default .)
   - :env (map, default System/getenv)
   - :required (set of keys)
   - :types (map of key -> type)
   - :allowed-keys (set/seq of dot-keys allowed from env)
   - :env-only? (when true, skip default file lookup)
   - :logger (fn [level data])

   Returns {:config <map> :meta <map>}"
  [{:keys [module-name config allow-relative? cwd env required types logger allowed-keys env-only?]}]
  (when-not (seq module-name)
    (throw (ex-info "module-name is required" {})))
  (let [{:keys [path source]} (select-config-file {:config config
                                                   :module-name module-name
                                                   :allow-relative? allow-relative?
                                                   :cwd cwd
                                                   :env-only? env-only?})
        file-config (if path (toml/read-toml-file path) {})
        env-map (env/env->config module-name (or env (System/getenv)) allowed-keys)
        merged (merge file-config env-map)
        typed (apply-types merged types)
        _ (validate-required typed required)
        meta {:file path
              :source source
              :env-keys (sort (keys env-map))}]
    (when logger
      (logger :info {:component ::configure
                     :event :config-loaded
                     :file path
                     :source source
                     :env-keys (:env-keys meta)}))
    {:config typed :meta meta}))

(defn dump-config
  "Returns config with secrets masked for safe output."
  ([config]
   (keys/mask-secrets config))
  ([config opts]
   (keys/mask-secrets config opts)))
