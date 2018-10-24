# colorbase

This is an example Clojure application that integrates with [Keybase proofs](http://keybase.io/docs/proof_integration_guide), allowing users to prove their identity on Keybase.

It uses the `clj-keybase-proofs` [library](https://github.com/keybase/clj-keybase-proofs).

You can run it locally by cloning the repository and running

```bash
$ boot run-dev-server -p 9090
```

if you have `boot` installed.

Of particular interest may be `handler.clj` and `api.clj`.

To create a secrets file encrypted with Keybase, run
```bash
$ boot make-secrets-config -n <your-keybase-username> -o secrets.edn.saltpack
```

Or, you could use whatever encryption scheme you like and change `decrypt.sh`
accordingly.
