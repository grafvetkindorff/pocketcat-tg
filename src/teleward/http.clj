(ns teleward.http
  (:require
   [cheshire.core :as json])
  (:import
   (java.io BufferedInputStream ByteArrayOutputStream)
   (java.util Base64)))


(defn buffered-input-stream-to-bytes [input-stream]
  (with-open [bis (BufferedInputStream. input-stream)
              baos (ByteArrayOutputStream.)]
    (let [buffer (byte-array 1024)]
      (loop []
        (let [read (.read bis buffer)]
          (when (pos? read)
            (.write baos buffer 0 read)
            (recur))))
      (.toByteArray baos))))

(defn base64-encode [file-bytes]
  (.encodeToString (Base64/getEncoder) file-bytes))

(defn make-options
  [api-key params data]
  (cond-> {:timeout 30000
           :headers {"Content-Type" "application/json"
                     "Authorization" (str "Bearer " api-key)}}
    (seq params)
    (assoc :query-params params)
    data
    (assoc :body (json/generate-string data {:pretty true}))))
