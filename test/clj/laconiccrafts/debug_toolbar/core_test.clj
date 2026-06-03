(ns laconiccrafts.debug-toolbar.core-test
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer :all]
    [laconiccrafts.debug-toolbar.core :as toolbar-core]))


(defn- html-request
  "Builds a minimal HTML request map for toolbar middleware tests."
  []
  {:request-method :get
   :uri "/admin/orders"
   :query-string "status=pending"
   :query-params {"status" "pending"}
   :params {"status" "pending"}
   :session {:identity {:name "Ada"}}
   :scheme :http
   :server-name "localhost"
   :server-port 3000})


(deftest wrap-debug-toolbar-injects-rendered-html
  (let [app
        (toolbar-core/wrap-debug-toolbar
          (fn [_request]
            (toolbar-core/append-sql-event!
              {:sql "select 1"
               :elapsed-ms 3})
            {:status 200
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body "<html><body><main>ok</main></body></html>"})
          {:enabled? true
           :renderer (fn [toolbar-data]
                       (str "<div id=\"visible-toolbar\">"
                         (get-in toolbar-data [:request :method])
                         "|"
                         (get-in toolbar-data [:totals :sql-count])
                         "</div>"))})
        response (app (html-request))]
    (is (str/includes? (:body response) "visible-toolbar"))
    (is (str/includes? (:body response) "GET|1"))))


(deftest wrap-debug-toolbar-renders-recorded-view-metadata
  (let [app
        (toolbar-core/wrap-debug-toolbar
          (fn [_request]
            (toolbar-core/record-view-render!
              {:view-id "auth/login.html"
               :view-path "/tmp/auth/login.html"
               :view-context {:debug-token "abc"}})
            {:status 200
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body "<html><body><main>ok</main></body></html>"})
          {:enabled? true})
        response (app (html-request))
        body (:body response)]
    (is (str/includes? body "View Template"))
    (is (str/includes? body "/tmp/auth/login.html"))
    (is (str/includes? body "View Context"))
    (is (str/includes? body "&quot;abc&quot;"))))


(deftest wrap-debug-toolbar-skips-htmx-fragments
  (let [app
        (toolbar-core/wrap-debug-toolbar
          (fn [_request]
            {:status 200
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body "<html><body><main>ok</main></body></html>"})
          {:enabled? true
           :renderer (constantly "<div id=\"visible-toolbar\"></div>")})
        response
        (app (assoc-in (html-request) [:headers "hx-request"] "true"))]
    (is (not (str/includes? (:body response) "visible-toolbar")))))


(deftest wrap-debug-toolbar-disabled-is-no-op
  (let [response {:status 200
                  :headers {"Content-Type" "text/html; charset=utf-8"}
                  :body "<html><body>ok</body></html>"}
        app
        (toolbar-core/wrap-debug-toolbar
          (fn [_request] response)
          {:enabled? false})]
    (is (= response (app (html-request))))))


(deftest template-path-resolves-default-html-root
  (let [path (toolbar-core/template-path "laconiccrafts/debug_toolbar_fixture.html")]
    (is (= (.getAbsolutePath
             (io/file "test/resources/html/laconiccrafts/debug_toolbar_fixture.html"))
          path))))


(deftest template-path-resolves-custom-resource-root
  (let [path
        (toolbar-core/template-path
          "custom-root"
          "laconiccrafts/custom_debug_toolbar_fixture.html")]
    (is (= (.getAbsolutePath
             (io/file
               "test/resources/custom-root/laconiccrafts/custom_debug_toolbar_fixture.html"))
          path))))
