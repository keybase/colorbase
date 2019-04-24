(ns colorbase.views
  (:require [colorbase.util :as util]
            [colorbase.config :refer [config]]
            [io.keybase.proofs :as keybase-proofs]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [hiccup.page :as page]
            [hiccup.element :refer [link-to]]
            [hiccup.form :refer [text-field submit-button hidden-field]]))

(defn get-miniform [uri value]
  [:form {:action uri :method :get} (submit-button value)])

(defn post-miniform
  ([uri value] (post-miniform uri value nil))
  ([uri value hidden-fields]
   [:form {:action uri :method :post}
    (anti-forgery-field)
    (if-not (empty? hidden-fields) (map (partial apply hidden-field) hidden-fields))
    (submit-button value)]))

(defn miniform-box [& miniforms]
  (vec (cons :div.miniform-box miniforms)))

(defn make-page-head
  ([] (make-page-head nil))
  ([username]
   [:head
    [:title (if username (util/colorify username) "Colorbase")]
    (page/include-css "/styles.css")
    (when username
      [:style (format "body { background-color: #%s; }" username)])]))

(defn login-form []
  [:form {:action "/login" :method :post :autocomplete :off}
   (anti-forgery-field)
   [:fieldset
    [:legend "Register or Login"]
    [:div
     [:label {:for :username} "Username "]
     [:input#username {:type :color :name :username :value (util/random-color)}]]
    [:div
     [:label {:for :password-1} "Password "]
     [:input#password-1 {:type :color :name :password-1 :value (util/random-color)}]
     [:input#password-2 {:type :color :name :password-2 :value (util/random-color)}]
     [:input#password-3 {:type :color :name :password-3 :value (util/random-color)}]]
    [:div
     (submit-button "Submit!")]]])

(defn logout-form [current-username]
  [:div.logout-box {:style (format "background-color: #%s;" current-username)}
   (get-miniform (str "/color/" current-username) "Profile!")
   (post-miniform "/logout" "Log out!")])

(defn user-box [{:keys [username keybase-proof-count]}]
  [:a.user-box
   {:href (str "/color/" username)
    :style (format "background: #%s;" username)
    :class (if (pos? keybase-proof-count) "has-keybase-proofs")}])

(defn render-homepage [users current-username]
  (page/html5
    (make-page-head)
    [:h1 "colorbase" [:small " colors for everyone!"]]
    (if current-username
      (logout-form current-username)
      (login-form))
    [:div.user-gallery (map user-box users)]))

(defn render-keybase-proof [username is-self {:keys [keybase-username sig-hash]}]
  (miniform-box
    (get-miniform (keybase-proofs/make-profile-link keybase-username sig-hash)
                  (str keybase-username "@keybase"))
    (when is-self
      (post-miniform "/api/delete-keybase-proof" "revoke" {:keybase-username keybase-username}))
    [:br]
    [:a {:href (keybase-proofs/make-profile-link keybase-username sig-hash)}
     [:img {:src (keybase-proofs/make-badge-link
                   (:domain-for-keybase config) username keybase-username sig-hash)}]]))

(defn render-profile [is-self {:keys [username keybase-proofs]}]
  (page/html5
    (make-page-head username)
    (map (partial render-keybase-proof username is-self) keybase-proofs)))

(defn render-create-keybase-proof [current-username keybase-username sig-hash kb-ua]
  (page/html5
    (make-page-head current-username)
    (miniform-box
      (post-miniform
        "/api/create-keybase-proof"
        (format "I am the Keybase user %s!" keybase-username)
        {:keybase-username keybase-username :sig-hash sig-hash
         :username current-username :kb-ua kb-ua})
      (get-miniform "/" "Cancel!"))))
