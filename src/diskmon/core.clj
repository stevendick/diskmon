(ns diskmon.core
  (:use
   [clojure.tools.cli :only [cli]]
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

;; the file systems

(def file-systems (file-stores))

;; seq of file system name/remaining space pairs

(defn remaining-space []
  (map (juxt (memfn toString) (memfn getUnallocatedSpace)) file-systems))

;; find all file systems matching predicate

(defn find-fs [pred]
  (into {} (for [[k v] (remaining-space) :when (pred v)] [k v])))

;; tweet them

;; config file should be in form of Clojure map with the mentioned keys
(def twitter-config
  (-> 
   "config.clj" 
   (slurp)
   (read-string)))

(def twitter-creds 
  (let [{:keys [app-consumer-key app-consumer-secret user-access-token user-access-token-secret]} twitter-config]
    (make-oauth-creds app-consumer-key 
                      app-consumer-secret 
                      user-access-token 
                      user-access-token-secret)))

(def host-name 
  (-> 
   (InetAddress/getLocalHost)
   (.getHostName)))

(defn tweet [m]
  (statuses-update :oauth-creds twitter-creds
                   :params      {:status m}))

(def default-size (* 1024 1024 1024)) ;; 1GB

(defn now [] (->
              (java.util.Date.)
              (.getTime)
              java.sql.Timestamp.))

(defn -main [& args]
  (let [options (cli args
                  ["-s" "--size" "disk size to check" :default default-size :parse-fn #(Long/valueOf %)]
                  ["-c" "--config" "location of config file" :default "config.clj"])
        m (first options)
        size (:size m)]
    (doseq [low-fs (find-fs (partial < size))]
      (tweet (str host-name ":" (key low-fs) " low on disk space " (now))))))
