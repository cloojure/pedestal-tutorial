(ns main
  (:use util)
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [tupelo.core :as t]))
(t/refer-tupelo)

(defn response [status body & {:as headers}]
  (vals->context status body headers))

(def ok       (partial response 200))
(def created  (partial response 201))
(def accepted (partial response 202))

;-----------------------------------------------------------------------------
; DB functions
(defonce database (atom {}))

(defn find-list-by-id [dbval db-id]
  (get dbval db-id))

(defn find-list-item-by-ids [dbval list-id item-id]
  (get-in dbval [list-id :items item-id] nil))

(defn list-item-add
  [dbval list-id item-id new-item]
  (if (contains? dbval list-id)
    (assoc-in dbval [list-id :items item-id] new-item)
    dbval))

(def db-interceptor
  {:name  :database-interceptor
   :enter (fn [context]
            (update context :request glue {:database @database}))
   :leave (fn [context]
            (if-let [[op & args] (grab :tx-data context)]
              (do
                (apply swap! database op args)
                (assoc-in context [:request :database] @database))
              context))})

;-----------------------------------------------------------------------------
; Domain functions

(defn make-list      [name] {:name name :items {}})
(defn make-list-item [name] {:name name :done? false})

;-----------------------------------------------------------------------------
; API Interceptors
(def echo
  {:name  :echo
   :enter (fn [context]
            (let [request  (grab :request context)
                  response (ok context)]
              (glue context (vals->context response))))})

(def entity-render
  {:name :entity-render
   :leave (fn [context]
            (if-let [item (grab :result context)]
              (glue context {:response (ok item)})
              context))})

(def list-create
  {:name  :list-create
   :enter (fn [context]
            (let [name     (get-in context [:request :query-params :name] "Unnamed List")
                  new-list (make-list name)
                  db-id    (str (gensym "l"))
                  url      (route/url-for :list-view :params {:list-id db-id})  ]
                             ; #todo should be:   (route/url-for* {:route-name :list-view
                             ; #todo                               :options {:params {:list-id db-id}}})
              (glue context {:response (created new-list "Location" url)
                             :tx-data  [assoc db-id new-list]})))})

(def list-view
  {:name :list-view
   :enter (fn [context]
            (if-let [db-id (get-in context [:request :path-params :list-id])]
              (if-let [the-list (find-list-by-id (get-in context [:request database]) db-id )]
                (glue context {:result the-list})
                context )
              context
              ))})

(def list-item-view
  {:name  :list-item-view
   :leave (fn [context]
            (if-let [list-id (get-in context [:request :path-params :list-id])]
              (if-let [item-id (get-in context [:request :path-params :item-id])]
                (if-let [item (find-list-item-by-ids (get-in context [:request :database]) list-id item-id)]
                  (glue context {:result item})
                  context )
                context )
              context))})

(def list-item-create
  {:name  :list-item-create
   :enter (fn [context]
            (if-let [list-id (fetch-in context [:request :path-params :list-id])]
              (let [name     (fetch-in context [:request :query-params :name] "Unnamed Item")
                    new-item (make-list-item name)
                    item-id  (str (gensym "i"))]
                (-> context
                  (glue {:tx-data [list-item-add list-id item-id new-item]})
                  (assoc-in [:request :path-params :item-id] item-id)))
              context))})

(def routes
  (route/expand-routes
    #{
      ["/todo"                   :post    [db-interceptor list-create]]
      ["/todo"                   :get     echo :route-name :list-query-form]
      ["/todo/:list-id"          :get     [entity-render db-interceptor list-view]]
      ["/todo/:list-id"          :post    [entity-render list-item-view db-interceptor list-item-create]]
      ["/todo/:list-id/:item-id" :get     [entity-render list-item-view db-interceptor ]]
      ["/todo/:list-id/:item-id" :put     echo :route-name :list-item-update]
      ["/todo/:list-id/:item-id" :delete  echo :route-name :list-item-delete]}))

(def service-map
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   8890})

(defn start []
  (http/start (http/create-server service-map)))

(defonce server (atom nil))

(defn test-request [verb url]
  (io.pedestal.test/response-for (grab ::http/service-fn @server) verb url))

(defn start-dev []
  (reset! server
    (http/start (http/create-server (glue service-map {::http/join? false})))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

(defn -main [& args]
  (start))
