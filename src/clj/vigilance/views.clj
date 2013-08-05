(ns vigilance.views
  (:require
    [hiccup
      [page :refer [html5]]
      [page :refer [include-js]]]))

(defn index-page []
  (html5
    [:head
      [:title "Hello World"]
      (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js")
      (include-js "/js/main.js")]
    [:body
      [:h1 "Hello World"]]))
