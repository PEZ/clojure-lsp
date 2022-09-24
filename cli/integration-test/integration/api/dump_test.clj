(ns integration.api.dump-test
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]
   [integration.helper :as h]
   [integration.lsp :as lsp]))

(lsp/clean-after-test)

(deftest dump-test
  (testing "dumping whole project return correct edn"
    (with-open [rdr (lsp/cli! "dump"
                              "--project-root" h/root-project-path)]
      (let [result (edn/read-string (slurp rdr))]
        (is (= [:classpath
                :analysis
                :dep-graph
                :findings
                :settings
                :project-root
                :source-paths]
               (keys result)))
        (is (h/assert-submap
              {:project-root h/root-project-path
               :source-paths [(h/project-path->canon-path "test")
                              (h/project-path->canon-path "src")]}
              result)))))
  (testing "Dumping as json filtering specific keys"
    (with-open [rdr (lsp/cli! "dump"
                              "--project-root" h/root-project-path
                              "--output" (str {:format :json
                                               :filter-keys [:project-root :source-paths]}))]
      (let [result (json/parse-string (slurp rdr))]
        (is (= {"project-root" h/root-project-path
                "source-paths" [(h/project-path->canon-path "test")
                                (h/project-path->canon-path "src")]}
               result))))))
