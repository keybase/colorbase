(ns clojure-keybase-proofs.views
  (:require [hiccup.page :as page]
            [crypto.random :as csprng]
            [ring.util.response :refer [response]]
            [clojure.string :as string]))

(defn gen-page-head [title]
  [:head
   [:title title]
   (page/include-css "/styles.css")])

(defn random-color []
  (str "#" (csprng/hex 3)))

(defn login-form []
  [:form {:action "/" :method "post"}
   [:fieldset
    [:legend "Register or Login"]
    [:div
     [:label {:for "username"} "Username "]
     [:input#username {:type "color" :name "username" :value (random-color)}]]
    [:div
     [:label {:for "password-1"} "Password "]
     [:input#password-1 {:type "color" :name "password-1" :value (random-color)}]
     [:input#password-2 {:type "color" :name "password-2" :value (random-color)}]]
    [:div
     [:input {:type "submit" :value "Submit!"}]]]])

(defn user-box [{:keys [username keybase-users]}]
  [:a {:href (str "/u/" username)}
   [:div.user-box
    {:style (format "background: #%s;" username)
     :class (if (not-empty keybase-users) "has-keybase-proofs")}]])

(defn homepage [users]
  (page/html5
    (gen-page-head "Colorbase")
    [:h1 [:a {:href "/"} "colorbase"] [:small " colors for everyone!"]]
    (login-form)
    [:br]
    (map user-box users)))

(defn render-keybase-user [is-self {:keys [keybase_username]}]
 [:div.keybase-user
  [:a {:href (format "https://keybase.io/%s" keybase_username)}
    (format "keybase/%s" keybase_username)]
  (if is-self
    [:span
     [:span " "]
     [:a {:href (format "https://keybase.io/%s" keybase_username)}
      "&#X2716;"]])])

(defn profile [is-self {:keys [username keybase-users]}]
  (page/html5
    (gen-page-head (str "Colorbase/" username))
    [:style (format "body { background-color: #%s; }" username)] ; TODO invalid
    (map (partial render-keybase-user is-self) keybase-users)))

(defn render-make-keybase-proof [keybase-username sig-hash]
  (let [domain "https://colorbase.modalduality.org"
        url "/api/prove-keybase-identity username=<USERNAME> keybase-username=%s sig-hash=%s"]
    (response (format (str "$ http POST " domain url) keybase-username sig-hash))))
