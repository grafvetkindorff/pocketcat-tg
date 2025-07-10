(ns teleward.gemini
  (:require
   [cheshire.core :as json]
   [org.httpkit.client :as http-kit]))


(def api-key "")


(defn make-query-params
  [data]
  {:contents [{:parts [{:text "Classify transactions into spending categories. For each transaction, provide a list of the top three suitable categories and assign a confidence rating (from 1 to 100) for each category. Additionally, include the amount and currency."}
                       {:inline_data {:mime_type "image/jpeg" :data data}}]}]
   :generationConfig {:response_mime_type "application/json"
                      :response_schema
                      {:type "ARRAY"
                       :items {:type "OBJECT"
                               :properties {:currency {:type "STRING"}
                                            :amount {:type "NUMBER"}
                                            :categories {:type "ARRAY"
                                                         :items {:type "OBJECT"
                                                                 :properties {:category {:type "STRING"}
                                                                              :confidence {:type "NUMBER"}}}}}}}}})

(defn make-options
  [data]
  {:timeout 10000
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string (make-query-params data) {:pretty true})})

(defn ask-image
  [base64-image]
  (http-kit/post
   (str "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" api-key)
   (make-options base64-image)
   (fn [{:keys [status headers body error]}] ;; asynchronous response handling
     (if error
       (println "Failed, exception is " error)
       (->> (cheshire.core/parse-string body true)
            :candidates
            first
            :content
            :parts
            (mapv #(-> % :text (cheshire.core/parse-string true))))))))
