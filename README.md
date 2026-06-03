# debug_toolbar

Reusable development debug toolbar for Ring applications.

## Features

- Dev-only Ring middleware that injects a floating debug panel into full HTML responses
- JDBC query capture via `datasource-proxy`
- Default English UI built in pure Clojure HTML/CSS/JS
- Hooks for applications to record route metadata and rendered view metadata

## Install

Use as a git dependency:

```clojure
{laconiccrafts/debug-toolbar
 {:git/url "https://github.com/laconiccrafts/debug_toolbar.git"
  :git/tag "v0.1.0"
  :git/sha "<sha>"}}
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

To record rendered views from your app:

```clojure
(laconiccrafts.debug-toolbar.core/record-view-render!
  {:view-id "auth/login.html"
   :view-path "/abs/path/to/auth/login.html"
   :view-context {:email "ada@example.com"}})
```

To capture SQL:

```clojure
(laconiccrafts.debug-toolbar.sql/wrap-datasource
  datasource
  {:enabled? true
   :name "my-app-debug-toolbar"})
```

## Testing

```bash
clj -M:test
```

