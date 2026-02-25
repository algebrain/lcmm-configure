(ns configure.core-test
  (:require [clojure.test :refer [deftest is]]
            [configure.core :as core]))

(defn- tmp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory "cfg" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- write-toml! [dir name content]
  (let [f (java.io.File. dir name)]
    (spit f content)
    (.getPath f)))

(deftest select-config-file-explicit-test
  (let [dir (tmp-dir)
        path (write-toml! dir "x.toml" "a = 1")
        {:keys [path source]} (core/select-config-file {:config path :allow-relative? true})]
    (is (= :explicit source))
    (is (.endsWith path "x.toml"))))

(deftest select-config-file-relative-disallowed-test
  (is (thrown? Exception
               (core/select-config-file {:config "./x.toml" :allow-relative? false}))))

(deftest select-config-file-defaults-test
  (let [dir (tmp-dir)
        _ (write-toml! dir "users_config.toml" "a = 1")
        {:keys [source]} (core/select-config-file {:module-name "users"
                                                   :allow-relative? true
                                                   :cwd (.getPath dir)})]
    (is (= :module-default source))))

(deftest select-config-file-global-test
  (let [dir (tmp-dir)
        _ (write-toml! dir "config.toml" "a = 1")
        {:keys [source]} (core/select-config-file {:module-name "users"
                                                   :allow-relative? true
                                                   :cwd (.getPath dir)})]
    (is (= :global-default source))))

(deftest load-config-merge-test
  (let [dir (tmp-dir)
        path (write-toml! dir "users_config.toml"
                          "[db]\nurl = \"file\"\nport = 1111\n")
        env {"USERS__DB__URL" "env"
             "USERS__DB__PORT" "5432"}
        {:keys [config]} (core/load-config {:module-name "users"
                                            :config path
                                            :env env
                                            :types {"db.port" :int}})]
    (is (= "env" (get config "db.url")))
    (is (= 5432 (get config "db.port")))))

(deftest load-config-required-test
  (is (thrown? Exception
               (core/load-config {:module-name "users"
                                  :env {}
                                  :required #{"db.url"}}))))

(deftest dump-config-test
  (is (= {"db.password" "***"}
         (core/dump-config {"db.password" "secret"}))))

(deftest select-config-file-env-only-test
  (let [dir (tmp-dir)
        _ (write-toml! dir "users_config.toml" "a = 1")
        {:keys [path source]} (core/select-config-file {:module-name "users"
                                                        :allow-relative? true
                                                        :cwd (.getPath dir)
                                                        :env-only? true})]
    (is (nil? path))
    (is (= :none source))))
