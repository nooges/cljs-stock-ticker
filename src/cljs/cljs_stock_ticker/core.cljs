(ns cljs-stock-ticker.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-http.client :as http]
              [cljs.core.async :refer [<!]]
              [cljs-stock-ticker.codec :as codec]
              [clojure.string :as string])
    (:require-macros [cljs.core.async.macros :refer [go]]))

(def yql-url "http://query.yahooapis.com/v1/public/yql?q=")

(defn cnbc-url [ticker-symbols]
  (string/join "&"
               ["http://quote.cnbc.com/quote-html-webservice/quote.htm?"
                (str "symbols=" (string/join "%7C" ticker-symbols))
                "requestmethod=quick"
                "fund=1"
                "noform=1"
                "exthrs=1"
                "extMode=ALL"
                "extendedMask=2"
                "output=json"]))

;; codec/form-encode seems to die on seeing non-alphanumerics
;; (defn cnbc-url-x [ticker-symbols]
;;   (str "http://quote.cnbc.com/quote-html-webservice/quote.htm?"
;;        (codec/form-encode
;;         {:symbols "AAPL"
;;          :requestMethod "quick"
;;          :fund 1
;;          :noform 1
;;          :exthrs 1
;;          :extMode "ALL"
;;          :extendedMask 2
;;          :output "json"})))

(defn yql-query [url]
  (str "select * from html where url=\"" url "\""))

;; (defn yql-request-url [url]
;;   (str yql-url
;;        (js/encodeURIComponent (yql-query url))
;;        "&format=json"
;;        "&callback=hello"))

;; (defn get-cnbc-data-via-yql-x [ticker-symbols]
;;   (prn (yql-request-url (cnbc-url ticker-symbols)))
;;   (go (let [response (<! (http/jsonp (yql-request-url (cnbc-url ticker-symbols))
;;                                    {:with-credentials? false}))]
;;         (prn response))))

(defn get-cnbc-data-via-yql [ticker-symbols]
  (go (let [response (<! (http/jsonp yql-url
                                   {:with-credentials? false
                                   :query-params
                                   {:q (yql-query (cnbc-url ticker-symbols))
                                    :format "json"
                                    :callback "callback"}}))]
        (prn (cljs.reader/read-string (get-in response [:body :query :results :body]))))))

(def sample-data
  [{:symbol "AAPL"
    :price 118.20
    :name "Apple"
    :change-price 3.45}
   {:symbol "NFLX"
    :price 125.32
    :name "Netflix",
    :change-price -2.11}])

;; -----------------
;; Display functions
(defn ticker-table-row [data]
  [:tr
   [:td (data :symbol)]
   [:td (data :name)]
   [:td (data :price)]])

(defn ticker-table []
  [:table
   (map ticker-table-row sample-data)])

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to Stock Ticker"]
   [:div [:a {:href "/about"} "go to about page"]]
   ;(prn (codec/form-encode {:test "fd s"}))
   ;(prn (cnbc-url-x ["AAPL" "NFLX"]))
   (get-cnbc-data-via-yql ["AAPL" "NFLX" "SPY"])
   (ticker-table)])

(defn about-page []
  [:div [:h2 "About cljs_stock_ticker"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (mount-root))
