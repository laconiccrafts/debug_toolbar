(ns laconiccrafts.debug-toolbar.default-ui
  (:require
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [laconiccrafts.debug-toolbar.render :as render]))


(def ^:private toolbar-css
  "Inline CSS used by the default debug toolbar UI."
  (str
    "#" render/toolbar-root-id " {"
    "position: fixed;"
    "right: 1rem;"
    "bottom: 1rem;"
    "z-index: 9999;"
    "font-family: ui-monospace, SFMono-Regular, Menlo, monospace;"
    "color: #f4f1ea;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-toggle] {"
    "display: inline-flex;"
    "align-items: center;"
    "gap: 0.5rem;"
    "padding: 0.75rem 1rem;"
    "border: 1px solid rgba(255, 255, 255, 0.12);"
    "border-radius: 999px;"
    "background: linear-gradient(135deg, #1f2937, #0f172a);"
    "box-shadow: 0 20px 35px rgba(15, 23, 42, 0.35);"
    "font-size: 0.75rem;"
    "font-weight: 700;"
    "letter-spacing: 0.04em;"
    "text-transform: uppercase;"
    "cursor: pointer;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-panel] {"
    "display: none;"
    "width: min(42rem, calc(100vw - 2rem));"
    "height: calc(100vh - 4.25rem);"
    "max-height: calc(100vh - 4.25rem);"
    "margin-top: 0.75rem;"
    "border: 1px solid rgba(255, 255, 255, 0.12);"
    "border-radius: 1.25rem;"
    "background: radial-gradient(circle at top left, rgba(249, 115, 22, 0.18), transparent 30%),"
    "linear-gradient(180deg, #111827, #020617);"
    "box-shadow: 0 28px 60px rgba(2, 6, 23, 0.45);"
    "overflow: hidden;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-panel].is-open {display: block;}"
    "#" render/toolbar-root-id " [data-toolbar-header] {"
    "padding: 1rem 1rem 0.75rem;"
    "border-bottom: 1px solid rgba(255, 255, 255, 0.08);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-subtitle] {"
    "margin: 0.2rem 0 0;"
    "font-size: 0.75rem;"
    "color: rgba(244, 241, 234, 0.68);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-tabs] {"
    "display: flex;"
    "flex-wrap: wrap;"
    "gap: 0.5rem;"
    "margin-top: 0.9rem;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-tab] {"
    "border: none;"
    "border-radius: 999px;"
    "padding: 0.5rem 0.85rem;"
    "background: rgba(255, 255, 255, 0.08);"
    "color: rgba(244, 241, 234, 0.76);"
    "cursor: pointer;"
    "font-size: 0.78rem;"
    "font-weight: 700;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-tab].is-active {"
    "background: #f97316;"
    "color: #111827;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-body] {"
    "padding: 1rem;"
    "overflow: auto;"
    "height: calc(100vh - 10.5rem);"
    "max-height: calc(100vh - 10.5rem);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-view] {display: none;}"
    "#" render/toolbar-root-id " [data-toolbar-view].is-active {display: block;}"
    "#" render/toolbar-root-id " [data-toolbar-metrics] {"
    "display: grid;"
    "grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));"
    "gap: 0.75rem;"
    "margin-bottom: 1rem;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-metric] {"
    "padding: 0.85rem;"
    "border-radius: 1rem;"
    "background: rgba(255, 255, 255, 0.06);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-metric-label] {"
    "display: block;"
    "font-size: 0.7rem;"
    "text-transform: uppercase;"
    "letter-spacing: 0.06em;"
    "color: rgba(244, 241, 234, 0.62);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-metric-value] {"
    "display: block;"
    "margin-top: 0.35rem;"
    "font-size: 0.95rem;"
    "font-weight: 700;"
    "word-break: break-word;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-block] + [data-toolbar-block] {margin-top: 1rem;}"
    "#" render/toolbar-root-id " [data-toolbar-block-title] {"
    "display: flex;"
    "align-items: center;"
    "justify-content: space-between;"
    "gap: 0.75rem;"
    "margin: 0;"
    "padding: 0.25rem 0;"
    "font-size: 0.8rem;"
    "font-weight: 700;"
    "color: rgba(244, 241, 234, 0.88);"
    "cursor: pointer;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-block-title]:focus-visible {"
    "outline: 2px solid rgba(249, 115, 22, 0.85);"
    "outline-offset: 0.2rem;"
    "border-radius: 0.35rem;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-block-title]::after {"
    "content: '−';"
    "flex-shrink: 0;"
    "font-size: 1rem;"
    "line-height: 1;"
    "color: rgba(244, 241, 234, 0.62);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-block].is-collapsed [data-toolbar-block-title]::after {"
    "content: '+';"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-block-content] {"
    "margin-top: 0.4rem;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-block].is-collapsed [data-toolbar-block-content] {"
    "display: none;"
    "}"
    "#" render/toolbar-root-id " pre {"
    "margin: 0;"
    "padding: 0.85rem;"
    "border-radius: 1rem;"
    "background: rgba(15, 23, 42, 0.78);"
    "color: #e5e7eb;"
    "font-size: 0.75rem;"
    "line-height: 1.45;"
    "white-space: pre-wrap;"
    "word-break: break-word;"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-sql-row] + [data-toolbar-sql-row] {margin-top: 1rem;}"
    "#" render/toolbar-root-id " [data-toolbar-sql-row] {"
    "padding: 0.9rem;"
    "border-radius: 1rem;"
    "background: rgba(255, 255, 255, 0.06);"
    "border: 1px solid rgba(255, 255, 255, 0.08);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-sql-row].is-slow {"
    "border-color: rgba(249, 115, 22, 0.6);"
    "background: rgba(249, 115, 22, 0.12);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-sql-meta] {"
    "display: flex;"
    "flex-wrap: wrap;"
    "gap: 0.6rem;"
    "margin-bottom: 0.75rem;"
    "font-size: 0.72rem;"
    "color: rgba(244, 241, 234, 0.7);"
    "}"
    "#" render/toolbar-root-id " [data-toolbar-empty] {"
    "padding: 0.9rem;"
    "border-radius: 1rem;"
    "background: rgba(255, 255, 255, 0.05);"
    "color: rgba(244, 241, 234, 0.7);"
    "font-size: 0.8rem;"
    "}"))


(def ^:private toolbar-js
  "Inline JS used by the default debug toolbar UI."
  (str
    "(function () {"
    "var root = document.getElementById('" render/toolbar-root-id "');"
    "if (!root || typeof window === 'undefined') { return; }"
    "var storageKey = '" render/toolbar-root-id ":open';"
    "var toggle = root.querySelector('[data-toolbar-toggle]');"
    "var panel = root.querySelector('[data-toolbar-panel]');"
    "var tabs = root.querySelectorAll('[data-toolbar-tab]');"
    "var views = root.querySelectorAll('[data-toolbar-view]');"
    "var blockTitles = root.querySelectorAll('[data-toolbar-block-title]');"
    "var defaultOpen = root.getAttribute('data-collapsed-by-default') !== 'true';"
    "var stored = window.localStorage.getItem(storageKey);"
    "var open = stored === null ? defaultOpen : stored === 'true';"
    "function setOpen(nextOpen) {"
    "open = !!nextOpen;"
    "panel.classList.toggle('is-open', open);"
    "window.localStorage.setItem(storageKey, String(open));"
    "}"
    "function activateTab(name) {"
    "tabs.forEach(function (tab) {"
    "tab.classList.toggle('is-active', tab.getAttribute('data-toolbar-tab') === name);"
    "});"
    "views.forEach(function (view) {"
    "view.classList.toggle('is-active', view.getAttribute('data-toolbar-view') === name);"
    "});"
    "}"
    "function setBlockExpanded(title, expanded) {"
    "var block = title.closest('[data-toolbar-block]');"
    "if (!block) { return; }"
    "block.classList.toggle('is-collapsed', !expanded);"
    "title.setAttribute('aria-expanded', String(expanded));"
    "}"
    "toggle.addEventListener('click', function () { setOpen(!open); });"
    "tabs.forEach(function (tab) {"
    "tab.addEventListener('click', function () {"
    "activateTab(tab.getAttribute('data-toolbar-tab'));"
    "});"
    "});"
    "blockTitles.forEach(function (title) {"
    "title.addEventListener('click', function () {"
    "setBlockExpanded(title, title.getAttribute('aria-expanded') !== 'true');"
    "});"
    "title.addEventListener('keydown', function (event) {"
    "if (event.key === 'Enter' || event.key === ' ') {"
    "event.preventDefault();"
    "setBlockExpanded(title, title.getAttribute('aria-expanded') !== 'true');"
    "}"
    "});"
    "setBlockExpanded(title, true);"
    "});"
    "activateTab('summary');"
    "setOpen(open);"
    "})();"))


(defn- html-escape
  "Escapes raw text for safe HTML interpolation."
  [value]
  (-> (str value)
    (str/replace "&" "&amp;")
    (str/replace "<" "&lt;")
    (str/replace ">" "&gt;")
    (str/replace "\"" "&quot;")
    (str/replace "'" "&#39;")))


(defn- pretty-html
  "Pretty-prints Clojure data and escapes it for safe HTML rendering."
  [value]
  (when (some? value)
    (html-escape
      (with-out-str
        (pprint/pprint value)))))


(defn- present?
  "Returns true when `value` should render as present content."
  [value]
  (cond
    (nil? value) false
    (string? value) (not (str/blank? value))
    :else true))


(defn- empty-state
  "Renders the standard empty-state message for a toolbar block."
  [copy]
  (str "<div data-toolbar-empty>"
    (html-escape (:no-data copy))
    "</div>"))


(defn- pre-or-empty
  "Wraps rendered content in a `<pre>` tag or falls back to the empty state."
  [copy rendered-value]
  (if (present? rendered-value)
    (str "<pre>" rendered-value "</pre>")
    (empty-state copy)))


(defn- block
  "Renders a titled toolbar block with collapsible body content."
  [title body]
  (str "<div data-toolbar-block>"
    "<h4 data-toolbar-block-title role=\"button\" tabindex=\"0\" aria-expanded=\"true\">"
    (html-escape title)
    "</h4>"
    "<div data-toolbar-block-content>"
    body
    "</div>"
    "</div>"))


(defn- metric
  "Renders a summary metric tile with escaped label and value text."
  [label value]
  (str "<div data-toolbar-metric>"
    "<span data-toolbar-metric-label>" (html-escape label) "</span>"
    "<span data-toolbar-metric-value>" (html-escape value) "</span>"
    "</div>"))


(defn- sql-row
  "Renders a single SQL event row, including statement, params, metadata, and errors."
  [copy slow-threshold-ms index event]
  (let [statement (or (:sql event)
                    (str/join "\n\n" (keep :sql (:queries event))))
        params (or (:params event)
                 (mapv :params (:queries event)))
        meta-data
        (select-keys event
          [:statement-type
           :success?
           :batch?
           :batch-size
           :query-size
           :data-source-name])]
    (str "<article data-toolbar-sql-row"
      (when (>= (:elapsed-ms event 0) slow-threshold-ms)
        " class=\"is-slow\"")
      ">"
      "<div data-toolbar-sql-meta>"
      "<span>#" (inc index) "</span>"
      "<span>" (:elapsed-ms event) "ms</span>"
      "</div>"
      (block (:sql-statement copy)
        (pre-or-empty copy (some-> statement html-escape)))
      (block (:sql-params copy)
        (pre-or-empty copy (pretty-html params)))
      (block (:sql-meta copy)
        (pre-or-empty copy (pretty-html meta-data)))
      (when-let [error-message (:error-message event)]
        (block (:sql-error copy)
          (pre-or-empty copy (html-escape error-message))))
      "</article>")))


(defn render-default-toolbar-html
  "Renders the shipped English debug toolbar UI from generic toolbar data."
  [toolbar-data]
  (let [ui-options (:ui-options toolbar-data)
        copy (merge render/default-copy (:copy ui-options))
        route-pre (pretty-html (:route toolbar-data))
        view-data (:view toolbar-data)
        view-path (some-> (:view-path view-data) html-escape)
        view-context-pre (pretty-html (:view-context view-data))
        request-pre (pretty-html (:request toolbar-data))
        response-pre (pretty-html (:response toolbar-data))
        params-pre (pretty-html (:params toolbar-data))
        session-pre (pretty-html (:session toolbar-data))
        identity-pre (pretty-html (:identity toolbar-data))
        flash-pre (pretty-html (:flash toolbar-data))
        sql-rows
        (map-indexed
          (partial sql-row copy
            (:slow-query-threshold-ms ui-options))
          (:sql toolbar-data))]
    (str "<div id=\"" render/toolbar-root-id "\" data-collapsed-by-default=\""
      (if (:collapsed-by-default? ui-options) "true" "false")
      "\">"
      "<style>" toolbar-css "</style>"
      "<button type=\"button\" data-toolbar-toggle>"
      "<span>" (html-escape (:title copy)) "</span>"
      "<span>" (get-in toolbar-data [:response :status]) "</span>"
      "<span>" (get-in toolbar-data [:response :elapsed-ms]) "ms</span>"
      "<span>" (get-in toolbar-data [:totals :sql-count]) " SQL</span>"
      "</button>"
      "<section data-toolbar-panel>"
      "<header data-toolbar-header>"
      "<div class=\"font-bold\">" (html-escape (:title copy)) "</div>"
      "<p data-toolbar-subtitle>" (html-escape (:subtitle copy)) "</p>"
      "<nav data-toolbar-tabs>"
      "<button type=\"button\" data-toolbar-tab=\"summary\">"
      (html-escape (:summary-tab copy))
      "</button>"
      "<button type=\"button\" data-toolbar-tab=\"params\">"
      (html-escape (:params-tab copy))
      "</button>"
      "<button type=\"button\" data-toolbar-tab=\"session\">"
      (html-escape (:session-tab copy))
      "</button>"
      "<button type=\"button\" data-toolbar-tab=\"sql\">"
      (html-escape (:sql-tab copy))
      "</button>"
      "</nav>"
      "</header>"
      "<div data-toolbar-body>"
      "<section data-toolbar-view=\"summary\">"
      "<div data-toolbar-metrics>"
      (metric (:request-metric copy)
        (str (get-in toolbar-data [:request :method])
          " "
          (get-in toolbar-data [:request :uri-with-query])))
      (metric (:status-metric copy)
        (get-in toolbar-data [:response :status]))
      (metric (:duration-metric copy)
        (str (get-in toolbar-data [:response :elapsed-ms]) "ms"))
      (metric (:sql-metric copy)
        (get-in toolbar-data [:totals :sql-count]))
      (metric (:sql-time-metric copy)
        (str (get-in toolbar-data [:totals :sql-total-ms]) "ms"))
      "</div>"
      (block (:route-label copy) (pre-or-empty copy route-pre))
      (block (:view-template-block copy) (pre-or-empty copy view-path))
      (block (:view-context-block copy) (pre-or-empty copy view-context-pre))
      (block (:request-block copy) (pre-or-empty copy request-pre))
      (block (:response-block copy) (pre-or-empty copy response-pre))
      "</section>"
      "<section data-toolbar-view=\"params\">"
      (block (:params-block copy) (pre-or-empty copy params-pre))
      "</section>"
      "<section data-toolbar-view=\"session\">"
      (block (:session-block copy) (pre-or-empty copy session-pre))
      (block (:identity-block copy) (pre-or-empty copy identity-pre))
      (block (:flash-block copy) (pre-or-empty copy flash-pre))
      "</section>"
      "<section data-toolbar-view=\"sql\">"
      (if (seq sql-rows)
        (apply str sql-rows)
        (empty-state copy))
      "</section>"
      "</div>"
      "</section>"
      "<script>" toolbar-js "</script>"
      "</div>")))
