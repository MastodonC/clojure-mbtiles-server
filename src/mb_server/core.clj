(ns mb-server.core
  (require [clojure.java.jdbc :as j]
           [clojure.java.jdbc.sql :as s]
           [compojure.route :as route]
           [compojure.handler :as handler]
           [clojure.java.io :as io]
           [clojure.data.json :as json]
           [clojure.math.numeric-tower :as math])
  (use compojure.core)
  (:import java.util.zip.InflaterInputStream))

(def db-belonging {:classname  "org.sqlite.JDBC",
          	     :subprotocol   "sqlite",
                         :subname	    "resources/sl_belonging.mbtiles"})


(defn zlib-decompress
  [input]
  (with-open [input (-> input io/input-stream InflaterInputStream.)]
    (slurp input)))

;; TILES
(defn tile-result [z x y]
  (j/with-connection db-belonging
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

(defn numbers-as-strings? [& strings]
;; (infof "Numbers as strings: %s" strings)
  (every? #(re-find #"^-?\d+(?:\.\d+)?$" %) strings))

;; TODO fix parsing to check whether it's a string first
(defn parse-double [txt]
;;  (infof "Parsing double: %s" txt)
  (Double/parseDouble txt))

;; flip y coordinate for grids
(defn flip-y [z y]
  (- (- (math/expt 2 (parse-double z)) 1)
     (parse-double y)))

(defn raw-grid-data [z x y]
  (j/with-connection db-belonging
    (j/with-query-results results
      ["SELECT grid FROM grids WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?" z x (flip-y z y) ]
      (first results))))

(defn raw-grid-tooltip-data [z x y]
  (j/with-connection db-belonging
    (j/with-query-results results
      ["SELECT key_name, key_json FROM grid_data WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?" z x (flip-y z y)]
      (doall results))))

(defn create-tooltip-data [d]
  {(:key_name d) (json/read-str (:key_json d) :key-fn keyword)})

(defn create-json-data [ds]
  (into {} (map create-tooltip-data ds)))

(defn join-grid-data [z x y]
  (let [g (json/read-str (zlib-decompress (:grid (raw-grid-data z x y))) :key-fn keyword)]
    {:keys (:keys g)
     :data (create-json-data (raw-grid-tooltip-data z x y))
     :grid (:grid g)}))

(defn format-grid-string [z x y]
  (str "grid(" (json/write-str (join-grid-data z x y)) ");"))

(defn grid-response [data]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body data})

(defn get-grid [z x y]
  (let [grid (raw-grid-data z x y)]
    (if (nil? grid)
      {:status 404}
      (grid-response (format-grid-string z x y)))))

;; TODO works in repl, doesn't work in browser. THrows java.lang.String cannot be cast to clojure.lang.IFn

;; ROUTES
(defroutes app-routes
  (GET "/api/:z/:x/:y.png" [z x y] (get-tile z x y))
  (GET "/api/:z/:x/:y.grid.json" [z x y] (get-grid z x y))
  (GET "/" [] "<h1>Hello World</h1>")
  (route/not-found "Page not found"))

(def app
  (handler/site app-routes))
