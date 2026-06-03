(ns laconiccrafts.debug-toolbar.sql
  (:require
    [laconiccrafts.debug-toolbar.core :as toolbar-core])
  (:import
    (javax.sql
      DataSource)
    (net.ttddyy.dsproxy
      ExecutionInfo
      QueryInfo)
    (net.ttddyy.dsproxy.listener
      QueryExecutionListener)
    (net.ttddyy.dsproxy.proxy
      ParameterSetOperation)
    (net.ttddyy.dsproxy.support
      ProxyDataSourceBuilder)))


(defn normalize-parameter-operation
  "Returns a plain-data representation of one prepared-statement parameter."
  [operation]
  (let [args (vec (or (.getArgs ^ParameterSetOperation operation) []))]
    {:method (some-> operation .getMethod .getName)
     :index (first args)
     :value (second args)
     :args args}))


(defn normalize-parameter-set
  "Returns one normalized batch entry from parameter set operations."
  [parameter-set]
  (mapv normalize-parameter-operation parameter-set))


(defn normalize-query-info
  "Returns a plain-data representation of one executed SQL query."
  [query-info]
  {:sql (.getQuery ^QueryInfo query-info)
   :params (mapv normalize-parameter-set
             (.getParametersList ^QueryInfo query-info))})


(defn normalize-query-execution
  "Returns a plain-data SQL event from datasource-proxy execution details."
  [execution-info query-info-list]
  (let [queries (mapv normalize-query-info query-info-list)
        single-query (when (= 1 (count queries))
                       (first queries))]
    {:data-source-name (.getDataSourceName ^ExecutionInfo execution-info)
     :statement-type (some-> execution-info .getStatementType str)
     :elapsed-ms (.getElapsedTime ^ExecutionInfo execution-info)
     :success? (.isSuccess ^ExecutionInfo execution-info)
     :batch? (.isBatch ^ExecutionInfo execution-info)
     :batch-size (.getBatchSize ^ExecutionInfo execution-info)
     :query-size (count queries)
     :error-message
     (some-> execution-info .getThrowable .getMessage)
     :queries queries
     :sql (:sql single-query)
     :params (:params single-query)}))


(defn query-listener
  "Builds datasource-proxy listener that appends SQL events to the collector."
  []
  (reify QueryExecutionListener
    (beforeQuery
      [_ _ _]
      nil)

    (afterQuery
      [_ execution-info query-info-list]
      (toolbar-core/append-sql-event!
        (normalize-query-execution execution-info query-info-list)))))


(defn wrap-datasource
  "Wraps `datasource` with datasource-proxy when SQL capture is enabled."
  [datasource {:keys [enabled? name]
               :or {name "debug-toolbar"}}]
  (if (and enabled?
        (instance? DataSource datasource))
    (-> (ProxyDataSourceBuilder/create ^DataSource datasource)
      (.name name)
      (.listener (query-listener))
      .build)
    datasource))

