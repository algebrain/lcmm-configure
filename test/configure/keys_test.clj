(ns configure.keys-test
  (:require [clojure.test :refer [deftest is]]
            [configure.keys :as keys]))

(deftest module-prefix-test
  (is (= "USERS__" (keys/module-prefix "users"))))

(deftest env-to-key-test
  (is (= "db.url" (keys/env->key "users" "USERS__DB__URL")))
  (is (nil? (keys/env->key "users" "OTHER__DB__URL"))))

(deftest flatten-map-test
  (is (= {"db.url" "x" "http.port" 8080}
         (keys/flatten-map {:db {:url "x"} :http {:port 8080}}))))

(deftest mask-secrets-test
  (is (= {"db.password" "***" "db.user" "u"}
         (keys/mask-secrets {"db.password" "p" "db.user" "u"}))))
