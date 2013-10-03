(ns mb-server.core
  (require [clojure.java.jdbc :as j]
           [clojure.java.jdbc.sql :as s]
           [compojure.route :as route]
           [compojure.handler :as handler]
           [clojure.java.io :as io]
           [clojure.math.numeric-tower :as math])
  (use compojure.core)
  (:import java.util.zip.InflaterInputStream))

(def db-path  "resources/average-belonging.mbtiles")
(def db-specs {:classname  "org.sqlite.JDBC",
          	     :subprotocol   "sqlite",
        	     :subname	    db-path})

(defn zlib-decompress
  [input]
  (with-open [input (-> input io/input-stream InflaterInputStream.)]
    (slurp input)))


;; TILES
(defn tile-result [z x y]
  (j/with-connection db-specs
    (j/with-query-results results ["SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?" z x y]
      (first results))))

(defn tile-response [tile]
  {:status 200
   :headers {"Content-Type" "image/png"}
   :body (new java.io.ByteArrayInputStream (:tile_data tile))})

(defn get-tile [z x y]
  (let [tile (tile-result z x y)]
    (if (nil? tile)
      {:status 404}
      (tile-response tile))))


;; GRIDS

;; flip y coordinate for grids
;; coord.row = Math.pow(2, coord.zoom) - coord.row - 1;
(defn flip-coord [z y]
  (- (math/expt 2 z) (- y 1)))


(defn grid-result [z x y]
  (j/with-connection db-specs
    (j/with-query-results results ["SELECT grid FROM grids WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?" z x y]
      (first results))))

(defn grid-response [data]
  :status 200
  {:headers {"Content-Type" "application/json"}
   :body data})

(defn get-grid [z x y]
  (let [grid (grid-result z x y)]
    (if (nil? grid)
      {:status 404}
      (grid-response (zlib-decompress (:grid grid))))))


;; ROUTES
(defroutes app-routes
  (GET "/api/:z/:x/:y.png" [z x y] (get-tile z x y))
  (GET "/api/:z/:x/:y.grid.json" [z x y] (get-grid z x y))
  (GET "/" [] "<h1>Hello World</h1>")
  (route/not-found "Page not found"))

(def app
  (handler/site app-routes))
