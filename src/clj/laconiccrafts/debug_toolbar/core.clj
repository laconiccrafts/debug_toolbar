(ns laconiccrafts.debug-toolbar.core
  (:require
    [clojure.string :as str]
    [laconiccrafts.debug-toolbar.default-ui :as default-ui]
    [laconiccrafts.debug-toolbar.inject :as inject]
    [laconiccrafts.debug-toolbar.render :as render]))


(def ^:dynamic *toolbar-collector*
  "Request-local atom used to collect debug toolbar events during a request."
  nil)


(def default-ui-options
  "Default reusable UI options shared by toolbar adapters."
  {:collapsed-by-default? true
   :slow-query-threshold-ms 100
   :include-session? true
   :include-identity? true})


(defn- request-method
  "Returns upper-case request method text."
  [request]
  (some-> request
    :request-method
    name
    str/upper-case))


(defn- request-uri
  "Builds request URI including query string when present."
  [request]
  (str (:uri request)
    (when-let [query-string (:query-string request)]
      (str "?" query-string))))


(defn- maybe-map
  "Returns `value` only when it is a non-empty map."
  [value]
  (when (and (map? value)
          (seq value))
    value))


(defn- request-params
  "Collects reusable request parameter snapshots."
  [request]
  (cond-> {}
    (maybe-map (:path-params request))
    (assoc :path (:path-params request))

    (maybe-map (:query-params request))
    (assoc :query (:query-params request))

    (maybe-map (:form-params request))
    (assoc :form (:form-params request))

    (maybe-map (:params request))
    (assoc :merged (:params request))

    (maybe-map (:parameters request))
    (assoc :coerced (:parameters request))))


(defn- request-summary
  "Builds the generic request summary payload."
  [request]
  {:method (request-method request)
   :uri (:uri request)
   :uri-with-query (request-uri request)
   :query-string (:query-string request)
   :scheme (some-> (:scheme request) name)
   :server-name (:server-name request)
   :server-port (:server-port request)})


(defn- response-summary
  "Builds the generic response summary payload."
  [response elapsed-ms]
  {:status (:status response)
   :content-type (or (get-in response [:headers "Content-Type"])
                   (get-in response [:headers "content-type"]))
   :elapsed-ms elapsed-ms})


(defn append-sql-event!
  "Appends a normalized SQL event to the current request collector."
  [sql-event]
  (when (and *toolbar-collector*
          sql-event)
    (swap! *toolbar-collector* update :sql-events
      (fnil conj [])
      sql-event)))


(defn record-view-render!
  "Stores rendered view metadata for the current request collector."
  [view-data]
  (when (and *toolbar-collector*
          view-data)
    (swap! *toolbar-collector* assoc :view view-data)))


(defn toolbar-data
  "Builds the stable plain-data contract consumed by renderers."
  [request response collector elapsed-ms route-info-fn ui-options]
  (let [ui-options (merge default-ui-options ui-options)
        sql-events (vec (:sql-events collector))
        route-info (when route-info-fn
                     (route-info-fn request))]
    {:request (request-summary request)
     :response (response-summary response elapsed-ms)
     :route route-info
     :view (:view collector)
     :params (request-params request)
     :session (when (:include-session? ui-options)
                (:session request))
     :identity (when (:include-identity? ui-options)
                 (or (:identity request)
                   (get-in request [:session :identity])))
     :flash (:flash request)
     :sql sql-events
     :totals {:sql-count (count sql-events)
              :sql-total-ms (reduce + 0 (map :elapsed-ms sql-events))}
     :ui-options ui-options}))


(defn wrap-debug-toolbar
  "Wraps a Ring handler with request-local toolbar collection and injection."
  [handler {:keys [enabled? renderer route-info-fn ui-options]}]
  (if-not enabled?
    handler
    (fn [request]
      (let [started-at (System/nanoTime)
            collector (atom {:sql-events []})
            response (binding [*toolbar-collector* collector]
                       (handler request))
            elapsed-ms
            (long (/ (- (System/nanoTime) started-at) 1000000))
            data (toolbar-data request
                   response
                   @collector
                   elapsed-ms
                   route-info-fn
                   ui-options)
            toolbar-html
            (render/render-toolbar-html
              (or renderer default-ui/render-default-toolbar-html)
              data)]
        (if (and toolbar-html
              (inject/full-html-response? request response))
          (inject/inject-html response toolbar-html)
          response)))))

