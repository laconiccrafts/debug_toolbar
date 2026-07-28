# Luminus Web With Leiningen

Add a dev-only toolbar namespace that centralizes enablement and app-specific
SQL datasource naming:

```clojure
(ns my-app.debug-toolbar
  (:require
    [my-app.config :refer [env]]
    [laconiccrafts.debug-toolbar.core :as toolbar]
    [laconiccrafts.debug-toolbar.sql :as toolbar-sql]))

(defn enabled?
  []
  (and (:dev env)
       (not (:async? env))
       (not= false (:debug-toolbar? env))))

(defn wrap-debug-toolbar
  [handler]
  (toolbar/wrap-debug-toolbar
    handler
    {:enabled? (enabled?)
     :ui-options {:collapsed-by-default? true}}))

(defn wrap-datasource
  [datasource]
  (toolbar-sql/wrap-datasource
    datasource
    {:enabled? (enabled?)
     :name "my-app-debug-toolbar"}))
```

In `env/dev/clj/my_app/dev_middleware.clj`, wrap the full app:

```clojure
(ns my-app.dev-middleware
  (:require
    [my-app.debug-toolbar :as debug-toolbar]
    [prone.middleware :refer [wrap-exceptions]]
    [ring.middleware.reload :refer [wrap-reload]]))

(defn wrap-dev
  [handler]
  (-> handler
      wrap-reload
      (wrap-exceptions {:app-namespaces ['my-app]})
      debug-toolbar/wrap-debug-toolbar))
```

In `env/dev/clj/my_app/env.clj`, add hooks to the existing Luminus `defaults`
map:

```clojure
(ns my-app.env
  (:require
    [laconiccrafts.debug-toolbar.core :as toolbar]
    [my-app.debug-toolbar :as debug-toolbar]))

(def defaults
  {:record-route! toolbar/record-route!
   :record-view-render! toolbar/record-view-render!
   :wrap-datasource debug-toolbar/wrap-datasource})
```

In `env/prod/clj/my_app/env.clj`, use no-op hooks so shared code can call the
same functions safely:

```clojure
(ns my-app.env
  (:require
    [laconiccrafts.debug-toolbar.core :as toolbar]))

(def defaults toolbar/noop-hooks)
```

For Reitit route data, record from endpoint middleware. Outer dev middleware
usually cannot see `:reitit.core/match`.

```clojure
(ns my-app.middleware
  (:require
    [laconiccrafts.debug-toolbar.core :as toolbar]))

(def router-data
  {:middleware [toolbar/wrap-reitit-route-info]})
```

Static resources and 404 responses can remain empty.

For Hiccup views, record the page function Var as `:view-id` and `:view-path`
because there is no `.html` template file:

```clojure
(ns my-app.layout
  (:require
    [hiccup2.core :as h]
    [laconiccrafts.debug-toolbar.core :as toolbar]
    [my-app.env :refer [defaults]]))

(defn dashboard-page
  [context]
  ((or (:record-view-render! defaults) toolbar/record-view-render!)
   #'dashboard-page
   context)
  (str
    (h/html
      [:html
       [:head
        [:title "Dashboard"]]
       [:body
        [:main "Dashboard"]]])))
```
