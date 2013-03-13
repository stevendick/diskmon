(ns diskmon.core
  (:use
   [twitter.oauth]
   [twitter.callbacks]
   [twitter.callbacks.handlers]
   [twitter.api.restful])
  (:import java.nio.file.FileSystems
           twitter.callbacks.protocols.SyncSingleCallback
           java.net.InetAddress)
  (:gen-class))

;; get the file-stores

(defn- file-stores []
   (-> 
     (FileSystems/getDefault)
     (.getFileStores)))

;; wrapper fn for method

(defn- fs-name [fs]
  (.name fs))

;; wrapper fn for method

(defn- free-space [fs]
  (.getUnallocatedSpace fs))

;; the file systems

(def file-systems (file-stores))

;; seq of file system name/remaining space pairs

(defn remaining-space []
  (mapcat (juxt fs-name free-space) file-systems))

;; blah

(defn less-than? [size entry]
  (< (val entry) size))

;; find all file systems matching predicate

(defn find-fs [pred]
  (filter pred (apply hash-map (remaining-space))))

;; tweet them

(def *app-consumer-key* "0UOWJfsnM007MYAgC31fw")

(def *app-consumer-secret* "abEGspPo5nK9JIDuuhFjkICfpYJb0szVtjcJScjTZ8")

(def *user-access-token* "1265394246-f3qMA9vXONhRqaE6VtZbvVGoKyhJM9ikOrxXnIg")

(def *user-access-token-secret* "NfmnHxXKlufvsg4eTCiBMTbxA1tAqUx9aoFzY9WpJI")

(def twitter-creds (make-oauth-creds *app-consumer-key*
                                     *app-consumer-secret*
                                     *user-access-token*
                                     *user-access-token-secret*))

(def host-name 
  (-> 
   (InetAddress/getLocalHost)
   (.getHostName)))

(defn tweet [m]
  (statuses-update :oauth-creds twitter-creds
                   :params      {:status m}))

(defn -main [& {:keys [size] :or {size 1000000}}]
  (doseq [low-fs (find-fs (partial less-than? size))]
    (println (str host-name ":" (key low-fs) " low on disk space"))))
