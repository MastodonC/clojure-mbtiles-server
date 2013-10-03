(defproject mb-server "0.1.0-SNAPSHOT"
  :description "MBTiles server"
  :url "https://github.com/MastodonC/clojure-mbtiles-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha5"]
		 [org.xerial/sqlite-jdbc "3.7.2"]
		 [compojure "1.1.5"]
                 [org.clojure/math.numeric-tower "0.0.2"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler mb-server.core/app}
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
