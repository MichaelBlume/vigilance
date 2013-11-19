(defproject vigilance "0.0.1"
  :description "A thingy to test motor vigilance"
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.match "0.2.0-rc5"]
                 [core.async "0.1.0-SNAPSHOT"]
                 [compojure "1.1.5"]
                 [jayq "2.4.0"]
                 [hiccup "1.0.4"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.8.6"]]
  :repositories {"sonatype-oss-public" "https://sonatype.org/content/groups/public"}
  :cljsbuild {
    :builds [{:source-paths ["src/cljs"]
              :compiler {:output-to "resources/public/js/main.js"
                         :optimizations :advanced
                         :externs ["resources/jquery-1.9.js"]
                         :pretty-print true}}]}
  :ring {:handler vigilance.routes/app})
