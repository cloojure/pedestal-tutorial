(ns hello                                        
  (:require
   ;[tupelo :as t]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]))
;(t/refer-tupelo)

(def unmentionables #{"YHWH" "Voldemort" "Mxyzptlk" "Rumplestiltskin" "曹操"})

(defn ok [body]
  {:status 200 :body body})

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

(def routes
  (route/expand-routes
    #{ ["/greet" :get respond-hello :route-name :greet] }))

(defn create-server []
  (http/create-server  {::http/routes routes
                        ::http/type :jetty
                        ::http/port 8890 } ))

(defn start []
  (http/start (create-server)))

