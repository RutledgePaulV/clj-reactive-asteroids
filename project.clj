(defproject clj-reactive-asteroids "0.1.0-SNAPSHOT"
  :description "A reactive programming approach to the game asteroids."
  :url "http://github.com/clj-reactive-asteroids"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [org.clojure/core.async "0.2.385"
                  :exclusions [org.clojure/tools.reader]]
                 [reagi "0.10.1"]
                 [rm-hull/monet "0.3.0"]
                 [binaryage/devtools "0.7.2"]]

  :plugins [[lein-figwheel "0.5.4-7"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:open-urls ["http://localhost:3449/index.html"]}
                :compiler {:main clj-reactive-asteroids.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/clj_reactive_asteroids.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/clj_reactive_asteroids.js"
                           :main clj-reactive-asteroids.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.4-7"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :source-paths ["src" "dev"]
                   :repl-options {:init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

)
