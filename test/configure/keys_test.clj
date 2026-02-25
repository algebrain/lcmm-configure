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

(deftest mask-secrets-default-test
  (is (= {"db.password" "***" "public.key" "pub"}
         (keys/mask-secrets {"db.password" "p" "public.key" "pub"}))))

(deftest mask-secrets-custom-test
  (is (= {"db.password" "p" "public.key" "***"}
         (keys/mask-secrets {"db.password" "p" "public.key" "pub"}
                            {:secret-key? (fn [k] (= "public.key" (str k)))}))))
