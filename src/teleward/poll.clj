(ns teleward.poll
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [teleward.processing :as processing]
   [teleward.state-atom :as state-atom]
   [teleward.telegram :as tg]
   [teleward.util :refer [with-safe-log]]))


(defn save-offset [offset-file offset]
  (spit offset-file (str offset)))

(defn load-offset [offset-file]
  (if (-> offset-file
          io/file
          .exists)
    (-> offset-file slurp Long/parseLong)
    (spit offset-file "")))


(defn run-polling
  [config]

  (let [{{:keys [update-timeout
                 offset-file]} :polling
         :keys [telegram]}
        config

        me
        (delay
          (tg/get-me telegram))

        state
        (state-atom/make-state)

        context
        {:me me
         :state state
         :telegram telegram
         :config config}

        offset
        (load-offset offset-file)]

    (loop [offset offset]

      (let [updates
            (with-safe-log
              (tg/get-updates telegram
                              {:offset offset
                               :timeout update-timeout}))

            new-offset
            (or (some-> updates peek :update_id inc)
                offset)]

        (log/debugf "Got %s updates, next offset: %s, updates: %s"
                    (count updates)
                    new-offset
                    (json/generate-string updates {:pretty true}))

        (when offset
          (save-offset offset-file new-offset))

        (processing/process-updates context updates)
        #_(processing/process-pending-users context)

        (recur new-offset)))))


(comment

  (def -telegram
    {:token "7054243567:AAFW_C_0MvVzd3lqyUZYGYszt2qaJuPEe9I"
     :user-agent "Clojure 1.10.3"
     :timeout (* 65 1000)
     :keepalive (* 65 1000)})

  (def -config {:telegram -telegram
                :polling {:offset-file "/tmp/TELEGRAM_OFFSET"}})

  (future (run-polling -config))

  ;; Via the API's getFile you can now get the required path information for the file:
  ;; https://api.telegram.org/bot<bot_token>/getFile?file_id=the_file_id
  ;; This will return an object with file_id, file_size and file_path. You can then use the file_path to download the file:
  ;; https://api.telegram.org/file/bot<token>/<file_path>

)
