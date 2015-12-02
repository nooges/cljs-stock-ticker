(ns cljs-stock-ticker.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-http.client :as http]
              [cljs.core.async :refer [<!]]
              [cljs-stock-ticker.codec :as codec]
              [clojure.string :as string]
              [cognitect.transit :as transit]
              [cljs.pprint]
              [clojure.walk :refer [keywordize-keys]])
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

(def r (transit/reader :json))

(defn get-cnbc-data-via-yql [ticker-symbols]
  (go (let [response (<! (http/jsonp yql-url
                                   {:with-credentials? false
                                   :query-params
                                   {:q (yql-query (cnbc-url ticker-symbols))
                                    :format "json"
                                    :callback "callback"}}))]
        (transit/read r (get-in response [:body :query :results :body])))))

;; Extract needed information from CNBC quote into common format
(defn convert-cnbc-quote [quote]
  (let [mappings {:symbol :symbol
                  :name :name
                  :last :last
                  :low :low
                  :high :high
                  :open :open
                  :list_time :last-time
                  :volume :volume
                  :change :change}]
    (into {} (map (fn [[old-key new-key]]
                    [new-key (old-key quote)])
                  mappings))))

(defn convert-cnbc-data [data]
  (let [quotes (get-in (keywordize-keys data) [:QuickQuoteResult :QuickQuote])]
    ;(cljs.pprint/pprint quotes)
    (map convert-cnbc-quote quotes)
    ))

(def sample-data
  [{:symbol "AAPL"
    :last 118.20
    :name "Apple"
    :change 3.45}
   {:symbol "NFLX"
    :last 125.32
    :name "Netflix",
    :change -2.11}])

;; -----------------
;; Display functions
(defn ticker-table-row [data]
  [:tr
   [:td (data :symbol)]
   [:td (data :name)]
   [:td (data :last)]])

(defn ticker-table [quotes]
  (cljs.pprint/pprint quotes)
  [:table
   (map ticker-table-row quotes)])
;; -------------------------
;; Views

(def state (atom {:quotes sample-data}))

(defn ticker-table-component []
  (ticker-table (@state :quotes)))

(defn update-ticker-table [quotes]
  (swap! state assoc :quotes quotes))

(defn home-page []
  [:div [:h2 "Welcome to Stock Ticker"]
   [:div [:a {:href "/about"} "go to about page"]]
   [ticker-table-component]
   ;(prn (codec/form-encode {:test "fd s"}))
   ;(prn (cnbc-url-x ["AAPL" "NFLX"]))
   (go (update-ticker-table (convert-cnbc-data (<! (get-cnbc-data-via-yql ["AAPL" "NFLX" "SPY"])))))])

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
