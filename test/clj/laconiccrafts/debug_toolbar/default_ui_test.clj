(ns laconiccrafts.debug-toolbar.default-ui-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [laconiccrafts.debug-toolbar.default-ui :as default-ui]
    [laconiccrafts.debug-toolbar.render :as render]))


(defn- sample-toolbar-data
  "Returns a generic toolbar payload for renderer behavior tests."
  []
  {:request {:method "GET"
             :uri "/login"
             :uri-with-query "/login"}
   :response {:status 200
              :elapsed-ms 18}
   :route {:template "/login"}
   :view {:view-id "auth/login.html"
          :view-path "/tmp/auth/login.html"
          :view-context {:debug-token "abc"}}
   :params {:query {"q" "1"}}
   :session {:identity {:name "Ada"}}
   :identity {:name "Ada"}
   :flash {:info "ok"}
   :sql [{:elapsed-ms 4
          :sql "select 1"
          :params []}]
   :totals {:sql-count 1
            :sql-total-ms 4}
   :ui-options {:collapsed-by-default? true
                :slow-query-threshold-ms 100
                :include-session? true
                :include-identity? true}})


(deftest render-default-toolbar-html-shows-visible-sections
  (let [html (default-ui/render-default-toolbar-html
               (sample-toolbar-data))]
    (is (str/includes? html "Debug"))
    (is (str/includes? html "Development only"))
    (is (str/includes? html "Summary"))
    (is (str/includes? html "SQL Queries"))
    (is (str/includes? html "Route"))
    (is (str/includes? html "/login"))
    (is (str/includes? html "View Template"))
    (is (str/includes? html "/tmp/auth/login.html"))
    (is (str/includes? html "View Context"))
    (is (str/includes? html "&quot;abc&quot;"))
    (is (str/includes? html "select 1"))
    (is (str/includes? html "data-toolbar-block-content"))
    (is (str/includes? html "data-toolbar-block-title role=\"button\" tabindex=\"0\" aria-expanded=\"true\""))
    (is (str/includes? html "setBlockExpanded"))
    (is (str/includes? html render/toolbar-root-id))))
