(ns hello                                        
  (:require
    [tupelo.core :as t]
    [clojure.data.json :as json]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [io.pedestal.http.content-negotiation :as conneg]))
(t/refer-tupelo)

(def unmentionables #{"YHWH" "Voldemort" "Mxyzptlk" "Rumplestiltskin" "曹操"})

(defn ok [body]
  {:status 200 :body body
   :headers {"Content-Type" "text/html"}})

(defn not-found []
  {:status 404 :body "Not Found \n"})

(defn greeting-for [name]
  (cond
    (unmentionables name) nil
    (empty? name) "Hello World!\n"
    :else (str "Hello, " name \newline)))

(defn respond-hello
  [request]
  (let [name (get-in request [:query-params :name])
        resp (greeting-for name)]
    (if resp
      (ok resp)
      (not-found))))

(def echo
  {:name  ::echo
   :enter #(into % {:response (ok (:request %))})})

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn accepted-type [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html"         body
    "text/plain"        body
    "application/edn"   (pr-str body)
    "application/json"  (json/write-str body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(def coerce-body
  {:name  ::coerce-body
   :leave (fn [context]
            (spyx :coerce-body context)
            (cond-> context
              (spy :nil? (nil? (get-in context [:response :headers "Content-Type"])))
              (update-in [:response] coerce-to (accepted-type context))))})

(def routes
  (route/expand-routes
    #{ ["/greet" :get [coerce-body content-neg-intc respond-hello] :route-name :greet]
       ["/echo"  :get echo] }))

(defn create-server []
  (http/create-server  {::http/routes routes
                        ::http/type :jetty
                        ::http/port 8890 } ))

(defn start []
  (http/start (create-server)))

(defn -main [& args]
  (start))
