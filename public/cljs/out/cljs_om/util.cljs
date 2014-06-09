(ns cljs-om.util
  (:require [clojure.string :as s]))

(defn search-hash []
  (subs (js/decodeURIComponent (aget js/window "location" "hash")) 2))

(defn number-format [number]
  "formats a number for display, e.g. 1.7K, 122K or 1.5M followers"
  (cond
    (< number 1000) (str number)
    (< number 100000) (str (/ (.round js/Math (/ number 100))10) "K")
    (< number 1000000) (str (.round js/Math (/ number 1000)) "K")
    :default (str (/ (.round js/Math (/ number 100000)) 10) "M")))

(defn from-now [date]
  "format date using the external moment.js library"
  (let [time-string (. (js/moment. date) (fromNow true))]
    (if (= time-string "a few seconds") "just now" time-string)))

(defn url-replacer [acc entity]
  "replace URL occurences in tweet texts with HTML (including links)"
  (s/replace acc (:url entity)
             (str "<a href='" (:url entity) "' target='_blank'>" (:display_url entity) "</a>")))

(defn hashtags-replacer [acc entity]
  "replace hashtags in tweet text with HTML (including links)"
  (let [hashtag (:text entity)]
    (s/replace acc (str "#" hashtag)
                         (str "<a href='https://twitter.com/search?q=%23" hashtag "' target='_blank'>#" hashtag "</a>"))))

(defn mentions-replacer [acc entity]
  "replace user mentions in tweet text with HTML (including links)"
  (let [screen-name (:screen_name entity)]
    (s/replace acc (str "@" screen-name)
               (str "<a href='http://www.twitter.com/" screen-name "' target='_blank'>@" screen-name "</a>"))))

(defn reducer [text coll fun]
  "generic reducer, allowing to call specified function for each item in collection"
  (reduce fun text coll))

(defn format-tweet [tweet]
  "format tweet text for display"
  (assoc tweet :html-text
    (-> (:text tweet)
        (reducer , (:urls (:entities tweet)) url-replacer)
        (reducer , (:user_mentions (:entities tweet)) mentions-replacer)
        (reducer , (:hashtags (:entities tweet)) hashtags-replacer)
        (s/replace , "RT " "<strong>RT </strong>"))))

(defn entity-count [tweet sym s]
  "gets count of specified entity from either tweet, or, when exists, original (retweeted) tweet"
  (let [rt-id (if (contains? tweet :retweeted_status) (:id_str (:retweeted_status tweet)) (:id_str tweet))
        count (sym ((keyword rt-id) (:retweets @cljs-om.core/app-state)))]
    (if (not (nil? count)) (str (number-format count) s) "")))

(defn rt-count [tweet] (entity-count tweet :retweet_count " RT | "))
(defn fav-count [tweet] (entity-count tweet :favorite_count " fav"))

(defn rt-count-since-startup [tweet]
  "gets RT count since startup for tweet, if exists returns formatted string"
  (let [t (if (contains? tweet :retweeted_status) (:retweeted_status tweet) tweet)
        count ((keyword (:id_str t)) (:rt-since-startup @cljs-om.core/app-state))]
    (if (> count 0) (str (number-format count) " RT since startup | ") "")))


(defn sorted-by [key-a key-b]
  "sorting function, initially comparing specified key and, if equal, favors higher ID"
  (fn [x y]
    (if (not (= (key-a x) (key-a y)))
      (> (key-a x) (key-a y))
      (> (key-b x) (key-b y)))))

(defn initial-state [] {:count 0        :n 10   :retweets {}
                        :tweets-map {}  :rt-since-startup {}
                        :search "*"     :stream nil
                        :sorted :by-id
                        :by-followers (sorted-set-by (sorted-by :followers_count :id))
                        :by-retweets (sorted-set-by (sorted-by :retweet_count :id))
                        :by-rt-since-startup (sorted-set-by (sorted-by :count :id))
                        :by-favorites (sorted-set-by (sorted-by :favorite_count :id))
                        :by-id (sorted-set-by >)
                        :words {}
                        :words-sorted-by-count (sorted-set-by (sorted-by :value :key))})

()
