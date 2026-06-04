# Debug Toolbar

Development debug toolbar for Ring applications, inspired by Django Debug Toolbar.

## Features

- Dev-only Ring middleware for full HTML responses
- HTMX fragment requests skipped automatically
- SQL capture via `datasource-proxy`
- Default UI in pure Clojure HTML/CSS/JS
- Optional route and rendered-view metadata hooks

## Install

Use as a git dependency:

```clojure
{laconiccrafts/debug-toolbar
 {:git/url "https://github.com/laconiccrafts/debug_toolbar.git"
  :git/tag "v0.3.0"
  :git/sha "b79f68ea0157f3142ffa4ae4eed457554162e9d1"}}
```

## Basic Usage

```clojure
(ns my.app.dev-toolbar
  (:require
    [laconiccrafts.debug-toolbar.core :as debug-toolbar]))

(defn route-info
  [request]
  {:template (get-in request [:reitit.core/match :template])})

(defn wrap-toolbar
  [handler]
  (debug-toolbar/wrap-debug-toolbar
    handler
    {:enabled? true
     :route-info-fn route-info
     :ui-options {:collapsed-by-default? true}}))
```

`wrap-debug-toolbar` injects toolbar HTML only into full `text/html` responses that contain `</body>`.

## Optional Integrations

Record rendered views from your app:

```clojure
(require '[laconiccrafts.debug-toolbar.core :as debug-toolbar])

(laconiccrafts.debug-toolbar.core/record-view-render!
  {:view-id "auth/login.html"
   :view-path (debug-toolbar/template-path "auth/login.html")
   :view-context {:email "ada@example.com"}})
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

## Kit Framework

For server-rendered Kit HTML apps, add toolbar middleware in `env/dev/clj/<app>/dev_middleware.clj` and follow [docs/kit-framework.md](docs/kit-framework.md) for full install, setup, and configuration steps.

## Testing

```bash
clj -M:test
```
