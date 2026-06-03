# Kit Framework Integration

Detailed setup for Kit applications that render server-side HTML pages.

This guide targets:

- generated Kit apps
- apps with HTML rendering enabled through `:kit/html` / Selmer
- optional SQL capture through Kit SQL components

## Prerequisites

This toolbar only injects into full HTML responses. API-only Kit apps will not show the toolbar unless they also render complete HTML pages with a closing `</body>` tag.

```bash
neil new io.github.kit-clj/kit yourname/guestbook
```

```bash
clojure -Tclj-new create :template io.github.kit-clj :name yourname/guestbook
```

If your Kit app does not already render HTML pages, install Kit HTML support first.

```clojure
(require '[kit.api :as kit])

(kit/sync-modules)
(kit/install-module :kit/html)
```

After installing a Kit module, restart your REPL before continuing.

## 1. Install Library

Add `debug_toolbar` to your project `deps.edn`.

```clojure
{:deps
 {laconiccrafts/debug-toolbar
  {:git/url "https://github.com/laconiccrafts/debug_toolbar.git"
   :git/tag "vX.X.X"
   :git/sha "<sha>"}}}
```

If `deps.edn` already has a `:deps` map, add only the `laconiccrafts/debug-toolbar` entry.

## 2. Enable Middleware

Add toolbar middleware in Kit development middleware so it runs only in development.

```clojure
(ns my-app.dev-middleware
  (:require
    [laconiccrafts.debug-toolbar.core :as debug-toolbar]))

(defn route-info
  [request]
  {:template (get-in request [:reitit.core/match :template])})

(defn wrap-dev
  [handler opts]
  (-> handler
      (debug-toolbar/wrap-debug-toolbar
        {:enabled? (= :dev (:profile opts))
         :route-info-fn route-info
         :ui-options {:collapsed-by-default? true
                      :slow-query-threshold-ms 100
                      :include-session? true
                      :include-identity? true}})))
```

`ui-options` controls only toolbar presentation and which request data gets shown. Current supported keys are:

- `:collapsed-by-default?` controls initial open state of toolbar panel.
- `:slow-query-threshold-ms` sets SQL timing threshold used by default UI to mark query row as slow. Queries at or above this number get highlighted in SQL tab.
- `:include-session?` controls whether request `:session` data is copied into toolbar payload and shown in Session tab. 
- `:include-identity?` controls whether authenticated user data is shown. When enabled, toolbar uses request `:identity` first, then falls back to `[:session :identity]` if present.

## 3. Record Rendered Views

The toolbar does not know which template rendered your page unless your app records that explicitly.

Create one shared layout helper and route all page rendering through it.

```clojure
(ns my-app.web.pages.layout
  (:require
    [laconiccrafts.debug-toolbar.core :as debug-toolbar]))

(defn render
  [opts request template context]
  (debug-toolbar/record-view-render!
    {:view-id template
     :view-path (debug-toolbar/template-path template)
     :view-context context})
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body ((get-in opts [:templating/selmer :render-file])
          template
          context)})
```

If your app already has a layout helper, keep its existing response-shaping logic and add only the `record-view-render!` call plus `:view-id`, `:view-path`, and `:view-context` data.

Update page routes to call the shared helper instead of calling Selmer directly.

```clojure
(ns my-app.web.routes.pages
  (:require
    [my-app.web.pages.layout :as layout]))

(defn home
  [opts request]
  (layout/render opts request "home.html"
    {:page-title "Home"}))

(defn page-routes
  [opts]
  [["/" {:get (partial home opts)}]])
```

If you render HTML from controllers, use same helper there too.

```clojure
(ns my-app.web.controllers.dashboard
  (:require
    [my-app.web.pages.layout :as layout]))

(defn index
  [{:keys [query-fn] :as opts} request]
  (layout/render opts request "dashboard.html"
    {:stats (query-fn :get-dashboard-stats {})}))
```

## 4. Capture SQL

Wrap Kit datasource component once, then point your query functions at wrapped datasource.

Load toolbar SQL namespace once so Integrant sees the shared init method.

```clojure
(ns my-app.db
  (:require
    [laconiccrafts.debug-toolbar.sql]))
```

That require registers `ig/init-key :db.sql/debug-connection`.

```clojure
[my-app.db]
```

Now add wrapped datasource component and point query execution at it.

Where this code goes: your app `resources/system.edn`.

```clojure
:db.sql/connection
#profile {:dev  {:jdbc-url "jdbc:postgresql://localhost/my_app?user=my_app&password=my_app"}
          :test {}
          :prod {:jdbc-url   #env JDBC_URL
                 :init-size  1
                 :min-idle   1
                 :max-idle   8
                 :max-active 32}}

:db.sql/debug-connection
{:datasource #ig/ref :db.sql/connection
 :enabled?   #profile {:dev true
                       :test false
                       :prod false}
 :name       "my-app-debug-toolbar"}

:db.sql/query-fn
{:conn     #ig/ref :db.sql/debug-connection
 :options  {}
 :filename "queries.sql"
 :env      #ig/ref :system/env}
```

For current Kit Conman setup, this works because `:db.sql/connection` resolves to a datasource-backed pooled connection object that `wrap-datasource` can decorate.

If your app already has a `:db.sql/migrations` entry, point that datasource at the wrapped connection too.

```clojure
:db.sql/migrations
{:store            :database
 :db               {:datasource #ig/ref :db.sql/debug-connection}
 :migrate-on-init? true}
```

If your app uses Hikari or a custom datasource key instead of `:db.sql/connection`, use same pattern:

- reference your real datasource component under `:datasource`
- keep `:db.sql/debug-connection` as wrapper layer
- point query and migration config at wrapped datasource key

## 5. Run And Verify

Start your Kit REPL and boot the app.

Where this command runs: terminal inside your Kit app.

```bash
clj -M:dev
```

Where this code goes: REPL inside your Kit app.

```clojure
(go)
```

Then verify behavior in browser:

- Load a normal HTML page such as `/`. Toolbar should appear.
- Hit a page that executes SQL. `SQL Queries` tab should list statements and timings.
- Hit a page rendered through `layout/render`. `View File` and `View Context` should be populated.
- Trigger an HTMX fragment request. Toolbar should not be injected into fragment response.

If toolbar does not appear, check these first:

- response content type starts with `text/html`
- response body contains a closing `</body>` tag
- request is not an HTMX fragment request
- `wrap-dev` is active in current `:dev` profile
- rendered page goes through your shared `layout/render` helper
- SQL queries use wrapped datasource instead of raw datasource

## Reference Notes

This guide matches current `debug_toolbar` behavior:

- toolbar injects only into full HTML responses
- HTMX fragment requests are skipped
- SQL capture happens through `laconiccrafts.debug-toolbar.sql/wrap-datasource`
- view metadata appears only when `laconiccrafts.debug-toolbar.core/record-view-render!` is called
