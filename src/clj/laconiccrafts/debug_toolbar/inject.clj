(ns laconiccrafts.debug-toolbar.inject
  (:require
    [clojure.string :as str]))


(def ^:private body-closing-tag
  "Closing body tag used as toolbar injection anchor."
  "</body>")


(defn htmx-request?
  "Returns true when request came from an HTMX fragment request."
  [request]
  (some? (get-in request [:headers "hx-request"])))


(defn full-html-response?
  "Returns true when response is a full HTML page eligible for injection."
  [request response]
  (let [content-type
        (some-> (or (get-in response [:headers "Content-Type"])
                  (get-in response [:headers "content-type"]))
          str/lower-case)
        body (:body response)]
    (and (not (htmx-request? request))
      (string? body)
      (some? content-type)
      (str/starts-with? content-type "text/html")
      (str/includes? body body-closing-tag))))


(defn inject-before-body-close
  "Appends `toolbar-html` right before the final closing body tag."
  [body toolbar-html]
  (let [index (.lastIndexOf ^String body body-closing-tag)]
    (if (neg? index)
      body
      (str (.substring ^String body 0 index)
        toolbar-html
        (.substring ^String body index)))))


(defn inject-html
  "Injects toolbar markup into a Ring HTML response."
  [response toolbar-html]
  (-> response
    (update :body inject-before-body-close toolbar-html)
    (update :headers
      (fn [headers]
        (dissoc (or headers {})
          "Content-Length"
          "content-length")))))

