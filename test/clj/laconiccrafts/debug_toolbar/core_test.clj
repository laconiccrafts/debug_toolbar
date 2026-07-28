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


(defn- html-page
  [_context]
  nil)


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


(deftest record-view-render-supports-id-and-context
  (let [collector (atom {})]
    (binding [toolbar-core/*toolbar-collector* collector]
      (toolbar-core/record-view-render! #'html-page {:email "ada@example.com"}))
    (is (= {:view-id #'html-page
            :view-path #'html-page
            :view-context {:email "ada@example.com"}}
          (:view @collector)))))


(deftest record-view-render-supports-explicit-path
  (let [collector (atom {})]
    (binding [toolbar-core/*toolbar-collector* collector]
      (toolbar-core/record-view-render!
        "auth/login.html"
        "/tmp/auth/login.html"
        {:email "ada@example.com"}))
    (is (= {:view-id "auth/login.html"
            :view-path "/tmp/auth/login.html"
            :view-context {:email "ada@example.com"}}
          (:view @collector)))))


(deftest wrap-debug-toolbar-renders-recorded-route-metadata
  (let [app
        (toolbar-core/wrap-debug-toolbar
          (fn [_request]
            (toolbar-core/record-route!
              {:method :get
               :template "/debug/:id"
               :path "/debug/123"
               :path-params {:id "123"}
               :parameters {:path {:id 123}}
               :route-data {:name ::debug}})
            {:status 200
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body "<html><body><main>ok</main></body></html>"})
          {:enabled? true})
        response (app (html-request))
        body (:body response)]
    (is (str/includes? body "Route"))
    (is (str/includes? body "/debug/:id"))
    (is (str/includes? body ":path-params"))
    (is (str/includes? body "&quot;123&quot;"))))


(deftest wrap-debug-toolbar-prefers-route-info-fn-over-recorded-route
  (let [app
        (toolbar-core/wrap-debug-toolbar
          (fn [_request]
            (toolbar-core/record-route!
              {:template "/recorded/:id"})
            {:status 200
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body "<html><body><main>ok</main></body></html>"})
          {:enabled? true
           :route-info-fn (fn [_request]
                            {:template "/explicit/:id"})})
        response (app (html-request))
        body (:body response)]
    (is (str/includes? body "/explicit/:id"))
    (is (not (str/includes? body "/recorded/:id")))))


(deftest wrap-debug-toolbar-shows-empty-route-when-unrecorded
  (let [app
        (toolbar-core/wrap-debug-toolbar
          (fn [_request]
            {:status 200
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body "<html><body><main>ok</main></body></html>"})
          {:enabled? true})
        response (app (html-request))
        body (:body response)]
    (is (str/includes? body "Route"))
    (is (str/includes? body "No data available."))))


(defn- reitit-request
  []
  (assoc (html-request)
    :request-method :get
    :path-params {:id "123"}
    :parameters {:path {:id 123}}
    :reitit.core/match
    {:template "/debug/:id"
     :result
     {:get
      {:path "/debug/123"
       :data {:name ::debug
              :handler ::handler
              :middleware [::middleware]
              :coercion ::coercion
              :parameters {:path {:id int?}}}}}}))


(deftest reitit-route-info-extracts-toolbar-route-data
  (is (= {:method :get
          :template "/debug/:id"
          :path "/debug/123"
          :path-params {:id "123"}
          :parameters {:path {:id 123}}
          :route-data {:name ::debug}}
        (toolbar-core/reitit-route-info (reitit-request)))))


(deftest record-reitit-route-records-current-match
  (let [collector (atom {})]
    (binding [toolbar-core/*toolbar-collector* collector]
      (toolbar-core/record-reitit-route! (reitit-request)))
    (is (= "/debug/:id"
          (get-in @collector [:route :template])))))


(deftest wrap-reitit-route-info-records-before-handler
  (let [seen-route (atom nil)
        handler (toolbar-core/wrap-reitit-route-info
                  (fn [_request]
                    (reset! seen-route
                      (some-> toolbar-core/*toolbar-collector* deref :route))
                    {:status 200
                     :headers {"Content-Type" "text/plain"}
                     :body "ok"}))
        collector (atom {})]
    (binding [toolbar-core/*toolbar-collector* collector]
      (handler (reitit-request)))
    (is (= "/debug/:id" (:template @seen-route)))))


(deftest noop-hooks-provide-disabled-app-defaults
  (let [sentinel (Object.)]
    (is (nil? ((:record-route! toolbar-core/noop-hooks) {:template "/"})))
    (is (nil? ((:record-reitit-route! toolbar-core/noop-hooks) (reitit-request))))
    (is (nil? ((:record-view-render! toolbar-core/noop-hooks)
               {:view-id "home.html"})))
    (is (identical? sentinel
          ((:wrap-datasource toolbar-core/noop-hooks) sentinel)))))


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


(deftest template-path-returns-jar-uri-for-embedded-resources
  (with-redefs [io/resource
                (fn [_path]
                  (java.net.URL.
                    "jar:file:/tmp/debug-toolbar.jar!/html/auth/login.html"))]
    (is (= "jar:file:/tmp/debug-toolbar.jar!/html/auth/login.html"
          (toolbar-core/template-path "auth/login.html")))))
