(defproject manifold-bot "0.1.0-SNAPSHOT"
  :description "A Clojure trading bot using manifold and core.async"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/core.async "1.3.618"]
                 [clj-http "3.12.3"]
                 [cheshire "5.10.1"]
                 [org.clojure/tools.logging "1.1.0"]]

  :source-paths ["src"]

  :main manifold-bot.core

  :profiles {:dev {:dependencies [[midje "1.9.9"]
                                  [lein-midje "3.2.1"]]}}

  :repl-options {:init-ns manifold-bot.core}

  :aot [manifold-bot.core]

  :target-path "target/%s")

