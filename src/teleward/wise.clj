(ns teleward.wise
  (:require
   [cheshire.core :as json]
   [org.httpkit.client :as http-kit]
   [teleward.http :refer [make-options]]))


(def api-key "")


(defn get-profile-id
  []
  (http-kit/get
   "https://api.transferwise.com/v2/profiles"
   (make-options api-key {} nil)
   (fn [{:keys [_status _headers body error]}] ;; asynchronous response handling
     (if error
       (println "Failed, exception is " error)
       (-> (cheshire.core/parse-string body true)
           first
           :id)))))

(defn get-balances
  [profile-id]
  (http-kit/get
   (format "https://api.transferwise.com/v4/profiles/%s/balances?types=STANDARD" profile-id)
   (make-options api-key {} nil)
   (fn [{:keys [_status _headers body error]}] ;; asynchronous response handling
     (if error
       (println "Failed, exception is " error)
       (-> (cheshire.core/parse-string body true))))))


(defn get-statement
  [profile-id balance-cfg interval-start interval-end]
  (http-kit/get
   (format "https://api.transferwise.com/v1/profiles/%s/balance-statements/%s/statement.json" profile-id (:id balance-cfg))
   (make-options api-key
                 {:currency (:currency balance-cfg)
                  :intervalStart interval-start
                  :intervalEnd interval-end
                  :type "COMPACT"}
                 nil)
   (fn [{:keys [_status _headers body error]}] ;; asynchronous response handling
     (if error
       (println "Failed, exception is " error)
       (do
         (println "status is " _status)
         (println "body is " body)
         (-> (cheshire.core/parse-string body true)))))))



(comment

  (def profile-id
    (http-kit/get
     "https://api.transferwise.com/v2/profiles"
     (make-options api-key nil nil)
     (fn [{:keys [_status _headers body error]}] ;; asynchronous response handling
       (if error
         (println "Failed, exception is " error)
         (do
           (println "body is " (cheshire.core/parse-string body true))
           (-> (cheshire.core/parse-string body true)
               first
               :id))))))

  profile-id

  (def balances @(get-balances profile-id))

  balances

  (def result (mapv
               (fn [balance]
                 @(get-statement profile-id balance "2025-01-01T00:00:00.000Z" "2025-01-05T00:00:00.000Z")) balances))

  result


  )