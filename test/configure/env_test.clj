(ns configure.env-test
  (:require [clojure.test :refer [deftest is]]
            [configure.env :as env]))

(deftest env-to-config-test
  (is (= {"db.url" "postgres"}
         (env/env->config "users" {"USERS__DB__URL" "postgres"})))
  (is (= {}
         (env/env->config "users" {"USERS__DB__URL" "  "})))
  (is (= {}
         (env/env->config "users" {"OTHER__DB__URL" "x"}))))

(deftest env-to-config-allowlist-test
  (is (= {"db.url" "postgres"}
         (env/env->config "users"
                          {"USERS__DB__URL" "postgres"
                           "USERS__DB__PORT" "5432"}
                          #{"db.url"}))))
