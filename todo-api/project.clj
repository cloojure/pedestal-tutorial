(defproject demo "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies  [
                  [io.pedestal/pedestal.jetty "0.5.1"]
                  [io.pedestal/pedestal.route "0.5.1"]
                  [io.pedestal/pedestal.service "0.5.1"]
                  [org.clojure/data.json "0.2.6"]
                  [org.clojure/test.check "0.9.0"]
                  [org.slf4j/slf4j-simple "1.7.21"]
                  [tupelo "0.9.53"]
                 ]
  :profiles {:dev     {:dependencies []}
             :uberjar {:aot :all}}
  :global-vars {*warn-on-reflection* false}
  :main ^:skip-aot main
  :target-path "target/%s"
  :jvm-opts ["-Xms1g" "-Xmx1g" ]
)
