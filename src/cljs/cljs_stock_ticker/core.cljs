(ns cljs-stock-ticker.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

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
