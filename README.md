# colorbase

This is an example Clojure application that integrates with [Keybase proofs](http://keybase.io/docs/proof_integration_guide), allowing users to prove their identity on Keybase.

It uses the `clj-keybase-proofs` [library](https://github.com/keybase/clj-keybase-proofs).

You can run it locally by cloning the repository and running

```clojure
$ boot run-dev-server -p 9090
```

if you have `boot` installed.

Of particular interest may be these handlers in `handler.clj`.

### List Keybase proofs for user
```clojure
(GET "/api/keybase-proofs" [username] (response (api/get-keybase-proofs-for-keybase username)
```
The handler which returns the list of keybase proofs for a user on Colorbase. This is pointed to
by the `check_url` and `check_path` in `keybase-config.json`.

### Allow user to create Keybase proof
```clojure
(GET "/create-keybase-proof" [keybase-username sig-hash :as request]
     (views/render-create-keybase-proof (:current-username request) keybase-username sig-hash))
```
This returns an HTML form that lets a user post a Keybase proof to your service, corresponding
to `prefill_url`. The parameters will be generated by the Keybase client, which will direct the user to this page.

### Backend support for creating Keybase proof
```clojure
(POST "/api/create-keybase-proof" [keybase-username sig-hash :as request]
	(api/create-keybase-proof
	  (:domain-for-keybase config) (:current-username request) keybase-username sig-hash)
	(redirect-in-web request "/color"))
```
This is called when the user submits the above form. It calls into the following function in `api.clj`.
On an error, you may want to let your users know that their Keybase proof is invalid and to try again.
```clojure
(defn create-keybase-proof [domain username keybase-username sig-hash]
  ; Check if the proof is valid. If not, error.
  (when-not (keybase-proofs/proof-valid? domain username keybase-username sig-hash)
    (throw (ex-info "401 Unauthorized. Invalid Keybase proof." {:code 403})))
  ; Add the proof to your database.
  ((:create-keybase-proof cmd) {:username username
                                :keybase-username keybase-username
                                :sig-hash sig-hash
                                :is-live false})
  ; At this point, requesting /api/keybase-proofs returns this proof, but it
  ; isn't marked as live yet, and so it isn't shown on users profiles.
  ; Now, check if the proof is live.
  (if (keybase-proofs/proof-live? domain username keybase-username sig-hash)
    ; If it is, mark the proof as live and start showing it on the user's profile pages.
    ((:enliven-keybase-proof cmd) {:username username
                                   :keybase-username keybase-username})
    ; Otherwise, keep it dead and return an error.
    (throw (ex-info "401 Unauthorized. Keybase proof not live." {:code 401}))))
```

### Allow users to delete proofs
Allow users to delete the proof on your website.
```clojure
(POST "/api/delete-keybase-proof" [keybase-username :as request]
	(api/delete-keybase-proof (:current-username request) keybase-username)
	(redirect-in-web request "/color"))
```
After this call, you can delete the proof or mark it as deleted in your
database. Do not return it in your `/api/keybase-proofs/` endpoint anymore.

### Display Keybase proof on user's profile page
Finally, a user's profile links to their Keybase profile and displays a status
badge. This function is in `views.clj`. Note that the status badge is optional,
and sends a request to the Keybase servers to get the current proof status and
render it in an SVG.
```clojure
(defn render-keybase-proof [username is-self {:keys [keybase-username sig-hash]}]
  (miniform-box
    (get-miniform (keybase-proofs/make-profile-link keybase-username sig-hash)
                  (str "@" keybase-username))
    (when is-self
      (post-miniform "/api/delete-keybase-proof" "delete!" {:keybase-username keybase-username}))
    [:br]
    [:a {:href (keybase-proofs/make-profile-link keybase-username sig-hash)}
     [:img {:src (keybase-proofs/make-badge-link
                   (:domain-for-keybase config) username keybase-username sig-hash)}]]))
```

### Sample database schema
You may also be interested in the simple HugSQL database schema needed to implement this functionality,
available at `src/colorbase/sql/schema.sql`.
