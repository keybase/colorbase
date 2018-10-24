(ns clojure-keybase-proofs.views
  (:require [hiccup.page :as page]))

(defn gen-page-head
  [title]
  [:head
   [:title "Colorbase"]
   (page/include-css "/styles.css")])

(defn homepage
  []
  (page/html5
    (gen-page-head "Home")
    [:h1 [:a {:href "/"} "Colorbase"]]
    [:h3 "Colors for everyone!"]
    [:ol
     [:li "Sign up with a color."]
     [:li "Connect with other users who have chosen different colors!"]]))
