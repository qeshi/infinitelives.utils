(ns infinitelives.utils.async
  (:require [cljs.core.async.macros :as m]
            [cljs.core.async.macros :refer [go]]))

;; macros for more convenient game async
(defmacro <!* [test & body]
  `(let [result# (cljs.core.async/<! ~@body)]
     (if ~test
       result#
       (throw (js/Error "go-while exit")))))

(defmacro >!* [test & body]
  `(let [result# (cljs.core.async/>! ~@body)]
     (if ~test
       result#
       (throw (js/Error "go-while exit")))))

(defn thread-test-around-sync-points [test body]
  (if (sequential? body)
    (let [[head & tail] body]
      (condp = head
        ;; thread test condition around <!
        '<!
        (thread-test-around-sync-points
         test
         (conj tail test 'infinitelives.utils.async/<!*))

        ;; thread test condition around >!
        '>!
        (thread-test-around-sync-points
         test
         (conj tail test 'infinitelives.utils.async/>!*))

        ;; other head
        (for [form body]
          (cond
            ;; recurse through subforms of a vector
            (vector? form)
            (into []
                  (thread-test-around-sync-points test form))

            ;; recurse through this full s-exp form
            (sequential? form)
            (thread-test-around-sync-points test form)

            ;; handle hashmap literals
            (map? form)
            (into {}
                  (for [[k v] form]
                   [(thread-test-around-sync-points test k)
                    (thread-test-around-sync-points test v)]))

            ;; default is leave code unchanged
            :default form))))
    body))

(defmacro continue-while [test & body]
  `(try
     ~(thread-test-around-sync-points test body)
     (catch js/Error e
       ;;(js/console.log "continue-while exit triggered by: " e)
       )))

(defmacro go-while [test & body]
  `(go
     (continue-while ~test ~@body)))
