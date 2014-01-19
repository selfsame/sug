(defproject sug "0.1.0-SNAPSHOT"
  :description "sugar for om"
  :url "https://github.com/selfsame/sug/"
  :license {:name "The MIT License (MIT)"
            :url "https://github.com/selfsame/sug/blob/master/LICENSE"}

  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138" :scope "provided"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha" :scope "provided"]
                 [om "0.1.7"]]

  :plugins [[lein-cljsbuild "0.3.4"]]

  :cljsbuild {:builds
              [
              {:id "simple"
				:source-paths ["src" "examples/simple/src"]
				:compiler {
					:output-to "examples/simple/main.js"
					:output-dir "examples/simple/out"
					:optimizations :none}}
               {:id "clickey-squares"
				:source-paths ["src" "examples/clickey-squares/src"]
				:compiler {
					:output-to "examples/clickey-squares/main.js"
					:output-dir "examples/clickey-squares/out"
					:optimizations :none}}
                           ]})



