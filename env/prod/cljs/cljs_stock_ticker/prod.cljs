(ns cljs-stock-ticker.prod
  (:require [cljs-stock-ticker.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
