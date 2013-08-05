(ns vigilance.routes
  (:use compojure.core
        vigilance.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes main-routes
  (GET "/" [] (index-page))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> main-routes
      handler/site
      wrap-base-url))
