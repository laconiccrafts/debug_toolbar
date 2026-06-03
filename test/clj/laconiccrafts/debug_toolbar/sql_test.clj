(ns laconiccrafts.debug-toolbar.sql-test
  (:require
    [clojure.test :refer :all]
    [integrant.core :as ig]
    [laconiccrafts.debug-toolbar.sql :as toolbar-sql])
  (:import
    (java.sql
      PreparedStatement)
    (java.io
      PrintWriter)
    (net.ttddyy.dsproxy
      ExecutionInfo
      QueryInfo)
    (net.ttddyy.dsproxy.proxy
      ParameterSetOperation)
    (javax.sql
      DataSource)))


(defn- set-string-method
  "Returns `PreparedStatement#setString(int,String)` reflection handle."
  []
  (.getMethod PreparedStatement
    "setString"
    (into-array Class [Integer/TYPE String])))


(deftest normalize-query-execution-returns-plain-data
  (let [operation
        (ParameterSetOperation.
          (set-string-method)
          (object-array [1 "Ada"]))
        query-info (doto (QueryInfo. "SELECT * FROM users WHERE name = ?")
                     (.setParametersList [[operation]]))
        execution-info (doto (ExecutionInfo.)
                         (.setDataSourceName "debug-toolbar")
                         (.setElapsedTime 12)
                         (.setSuccess true)
                         (.setBatch false)
                         (.setBatchSize 0))
        event
        (toolbar-sql/normalize-query-execution execution-info [query-info])]
    (is (= "debug-toolbar" (:data-source-name event)))
    (is (= 12 (:elapsed-ms event)))
    (is (= "SELECT * FROM users WHERE name = ?" (:sql event)))
    (is (= "setString"
          (get-in event [:params 0 0 :method])))
    (is (= 1 (get-in event [:params 0 0 :index])))
    (is (= "Ada" (get-in event [:params 0 0 :value])))))


(deftest wrap-datasource-disabled-is-no-op
  (let [sentinel (Object.)]
    (is (identical? sentinel
          (toolbar-sql/wrap-datasource sentinel
            {:enabled? false})))))


(defn- stub-datasource
  "Returns a minimal datasource for wrapper tests."
  []
  (proxy [javax.sql.DataSource] []
    (getConnection
      ([] (throw (UnsupportedOperationException.)))
      ([_ _] (throw (UnsupportedOperationException.))))
    (getLogWriter []
      nil)
    (setLogWriter [^PrintWriter _]
      (do))
    (setLoginTimeout [_]
      (do))
    (getLoginTimeout []
      0)
    (getParentLogger []
      (java.util.logging.Logger/getGlobal))
    (unwrap [_]
      nil)
    (isWrapperFor [_]
      false)))


(deftest integrant-debug-connection-wraps-datasource
  (let [datasource (stub-datasource)
        wrapped (ig/init-key :db.sql/debug-connection
                  {:datasource datasource
                   :enabled? true
                   :name "toolbar-test"})]
    (is (instance? DataSource wrapped))
    (is (not (identical? datasource wrapped)))))
