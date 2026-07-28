# Debug Toolbar

Development debug toolbar for Ring applications, inspired by Django Debug Toolbar.

## Features

- Dev-only Ring middleware for full HTML responses
- HTMX fragment requests skipped automatically
- SQL capture via `datasource-proxy`
- Default UI in pure Clojure HTML/CSS/JS
- Optional route and rendered-view metadata hooks

## Install

With `deps.edn`, use as a git dependency:

```clojure
{laconiccrafts/debug-toolbar
 {:git/url "https://github.com/laconiccrafts/debug_toolbar.git"
  :git/tag "v0.3.1"
  :git/sha "0bdf4a1ad0a8eb56a7833b3ce2ed7559ccf89be6"}}
```

With Leiningen/Luminus, use a published Maven artifact when available. If no
artifact is available, vendor or checkout the library source and add it to the
dev profile source paths:

```clojure
:profiles
{:dev
 {:source-paths ["env/dev/clj"
                 "vendor/debug_toolbar/src/clj"]
  :dependencies [[integrant/integrant "0.13.1"]
                 [net.ttddyy/datasource-proxy "1.11.0"]]}}
```

Leiningen does not consume `deps.edn` git coordinates directly.

## Basic Usage

```clojure
(ns my.app.dev-toolbar
  (:require
    [laconiccrafts.debug-toolbar.core :as debug-toolbar]))

(defn wrap-toolbar
  [handler]
  (debug-toolbar/wrap-debug-toolbar
    handler
    {:enabled? true
     :route-info-fn debug-toolbar/reitit-route-info
     :ui-options {:collapsed-by-default? true}}))
```

`wrap-debug-toolbar` injects toolbar HTML only into full `text/html` responses that contain `</body>`.

## Optional Integrations

Record matched routes from framework middleware:

```clojure
(require '[laconiccrafts.debug-toolbar.core :as debug-toolbar])

(debug-toolbar/record-reitit-route! request)
```

Use `:route-info-fn` when the wrapped request already contains the matched
route. Use `record-reitit-route!` or `wrap-reitit-route-info` when route data
only exists inside endpoint middleware, such as Reitit middleware in a Luminus
app.

Record rendered views from your app:

```clojure
(require '[laconiccrafts.debug-toolbar.core :as debug-toolbar])

(debug-toolbar/record-view-render!
  "auth/login.html"
  (debug-toolbar/template-path "auth/login.html")
  {:email "ada@example.com"})
```

Resolve template files from a different classpath root:

```clojure
(debug-toolbar/template-path "templates" "emails/welcome.html")
```

`template-path` returns an absolute path for file-backed resources and a
URI string for embedded resources such as uberjar entries.

Capture SQL by wrapping your datasource directly:

```clojure
(laconiccrafts.debug-toolbar.sql/wrap-datasource
  datasource
  {:enabled? true
   :name "my-app-debug-toolbar"})
```

Or let Integrant initialize the wrapped datasource for you:

```clojure
(ns my-app.db
  (:require
    [laconiccrafts.debug-toolbar.sql]))
```

```clojure
:db.sql/debug-connection
{:datasource #ig/ref :db.sql/connection
 :enabled?   #profile {:dev true
                       :test false
                       :prod false}
 :name       "my-app-debug-toolbar"}
```

Requiring `laconiccrafts.debug-toolbar.sql` registers
`ig/init-key :db.sql/debug-connection`.

Full SQL and rendered-view setup examples live in
[docs/kit-framework.md](docs/kit-framework.md).

## Luminus Web With Leiningen

For Luminus Web apps using Leiningen, add dev-only toolbar middleware and
follow [docs/luminus-leiningen.md](docs/luminus-leiningen.md) for full setup
and configuration steps.

## Kit Framework

For server-rendered Kit HTML apps, add toolbar middleware in `env/dev/clj/<app>/dev_middleware.clj` and follow [docs/kit-framework.md](docs/kit-framework.md) for full install, setup, and configuration steps.

## Testing

```bash
clj -M:test
```
