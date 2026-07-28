(ns laconiccrafts.debug-toolbar.core
  (:require
    [clojure.java.io :as io]
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


(defn- resource-location
  "Returns a stable display location for a classpath resource."
  [resource]
  (when resource
    (let [uri (.toURI resource)]
      (if (= "file" (.getScheme uri))
        (.getAbsolutePath (java.io.File. uri))
        (str uri)))))


(defn template-path
  "Returns a displayable location string for a classpath template resource.

  File-backed resources return an absolute filesystem path. Embedded
  resources, such as jar entries, return their URI string.

  With one arg, resolves `template` under the default `html/` root.
  With two args, resolves `template` under `resource-root/`."
  ([template]
   (template-path "html" template))
  ([resource-root template]
   (some-> (io/resource
             (str (str/replace (or resource-root "")
                    #"/*$"
                    "")
               "/"
               template))
     resource-location)))


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
  ([view-data]
   (when (and *toolbar-collector*
           view-data)
     (swap! *toolbar-collector* assoc :view view-data)))
  ([view-id view-context]
   (record-view-render!
     {:view-id view-id
      :view-path view-id
      :view-context view-context}))
  ([view-id view-path view-context]
   (record-view-render!
     {:view-id view-id
      :view-path view-path
      :view-context view-context})))


(defn record-route!
  "Stores matched route metadata for the current request collector."
  [route-data]
  (when (and *toolbar-collector*
          route-data)
    (swap! *toolbar-collector* assoc :route route-data)))


(defn- reitit-request-method
  "Returns lower-case request method keyword for Reitit endpoint lookup."
  [request]
  (some-> (:request-method request)
    name
    str/lower-case
    keyword))


(defn- reitit-route-endpoint
  "Returns the matched Reitit endpoint for the request method."
  [request]
  (get-in request [:reitit.core/match :result (reitit-request-method request)]))


(defn reitit-route-info
  "Extracts toolbar route metadata from a Reitit request match."
  [request]
  (when-let [match (:reitit.core/match request)]
    (let [endpoint (reitit-route-endpoint request)
          route-data (-> (:data endpoint)
                       (dissoc :handler
                         :middleware
                         :coercion
                         :parameters))]
      {:method (reitit-request-method request)
       :template (:template match)
       :path (:path endpoint)
       :path-params (:path-params request)
       :parameters (:parameters request)
       :route-data route-data})))


(defn record-reitit-route!
  "Records Reitit route metadata from the current request when present."
  [request]
  (when-let [route-data (reitit-route-info request)]
    (record-route! route-data)))


(defn wrap-reitit-route-info
  "Wraps a sync Ring handler and records matched Reitit route metadata."
  [handler]
  (fn [request]
    (record-reitit-route! request)
    (handler request)))


(def noop-hooks
  "No-op hooks for shared app code when the toolbar is disabled."
  {:record-route! (fn [_route-data] nil)
   :record-reitit-route! (fn [_request] nil)
   :record-view-render! (fn [_view-data] nil)
   :wrap-datasource identity})


(defn toolbar-data
  "Builds the stable plain-data contract consumed by renderers."
  [request response collector elapsed-ms route-info-fn ui-options]
  (let [ui-options (merge default-ui-options ui-options)
        sql-events (vec (:sql-events collector))
        route-info (when route-info-fn
                     (route-info-fn request))]
    {:request (request-summary request)
     :response (response-summary response elapsed-ms)
     :route (or route-info
              (:route collector))
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
