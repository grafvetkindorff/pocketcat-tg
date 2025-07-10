(ns teleward.chatgpt
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [org.httpkit.client :as http-kit]
   [teleward.http :refer [base64-encode buffered-input-stream-to-bytes
                          make-options]]))


(def api-key "")


(defn make-query-params
  [data]
  {:model "gpt-4o"
   :messages
   [{:role "user"
     :content [{:type "text"
                :text "Classify transactions into spending categories. List the top three suitable categories for each transaction and assign a confidence rating (1 to 10) for each category. Additionally, include the amount and currency."}
               {:type "image_url"
                :image_url {:url (str "data:image/jpeg;base64," data)}}]}]
   :response_format
   {:type "json_schema"
    :json_schema
    {:name "list_of_transactions"
     :strict true
     :schema {:type "object"
              :properties
              {:transactions
               {:type "array"
                :items
                {:type "object"
                 :properties {:currency {:type "string"}
                              :amount {:type "number"}
                              :categories {:type "array"
                                           :items {:type "object"
                                                   :properties {:category {:type "string"}
                                                                :confidence {:type "number"}}
                                                   :required ["category" "confidence"]
                                                   :additionalProperties false}}}
                 :required ["currency" "amount" "categories"]
                 :additionalProperties false}}}
              :required ["transactions"]
              :additionalProperties false}}}
   :max_tokens 300})

(defn ask-image
  [base64-image]
  (http-kit/post
   "https://api.openai.com/v1/chat/completions"
   (make-options api-key nil (make-query-params base64-image))
   (fn [{:keys [_status _headers body error]}] ;; asynchronous response handling
     (if error
       (println "Failed, exception is " error)
       (-> (cheshire.core/parse-string body true)
           (get-in [:choices 0 :message :content])
           (cheshire.core/parse-string true))))))

(defn process-file [file-path]
  (let [input-stream (io/input-stream file-path)
        file-bytes (buffered-input-stream-to-bytes input-stream)
        base64-image (base64-encode file-bytes)]
    (ask-image base64-image)))

(comment
  (def res (process-file "test-photo.jpg"))

  )
