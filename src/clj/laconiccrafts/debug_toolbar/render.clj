(ns laconiccrafts.debug-toolbar.render
  (:require
    [clojure.string :as str]))


(def toolbar-root-id
  "Stable DOM id used by debug-toolbar renderers."
  "debug-toolbar")


(def default-copy
  "Default English copy used by the shipped toolbar UI."
  {:title "Debug"
   :subtitle "Development only"
   :summary-tab "Summary"
   :params-tab "Params"
   :session-tab "Session"
   :sql-tab "SQL Queries"
   :request-metric "Request"
   :status-metric "Status"
   :duration-metric "Total Time"
   :sql-metric "Queries"
   :sql-time-metric "SQL Time"
   :route-label "Route"
   :view-template-block "View Template"
   :view-context-block "View Context"
   :request-block "Request"
   :response-block "Response"
   :params-block "Params"
   :session-block "Session"
   :identity-block "Identity"
   :flash-block "Flash"
   :sql-statement "SQL"
   :sql-params "Params"
   :sql-meta "Meta"
   :sql-error "Error"
   :no-data "No data available."})


(defn render-toolbar-html
  "Calls `renderer` with `toolbar-data`, returning nil for blank output."
  [renderer toolbar-data]
  (let [html (when renderer
               (renderer toolbar-data))]
    (when-not (str/blank? html)
      html)))

