(ns chipper.ui
  (:require [chipper.chips :as c]
            [chipper.state :as s]
            [chipper.notes :as n]
            [goog.string :as gs]
            [goog.string.format]
            [reagent.core :as r]))


;; TODO FIXME: everything is relative to :C right now.


(defn note-name [semitone] (name (n/name-rel :C semitone)))

(defn scheme-line [scheme]
  [:pre#scheme
   (apply str
          "    "  ;; length-3 padding...
          (for [instrument scheme]
            (gs/format " %-11s" (name instrument))))])

(defn edit-mode [mode]
  [:span {:class (when (= :edit mode) "bright-text")}
   (str " " (name mode))])

(defn modeline [left right state]
  [:pre#modeline.flex-spread
   [:span#modeline-left [edit-mode (:mode @state)] left]
   [:span#modeline-right
    [:label {:class :button :for :file} "load"]
    " "
    [:span {:id :save
            :class (str "button"
                        (when (:frame-edited @state) " bright-text"))
            :on-click #(s/save! state)}  ; TODO FIXME don't call save-state directly, make it an action.
     "save"]
    " "
    [:span {:id :play
            :class (str "button"
                        (when (:track-chan (:player @state))
                          " bright-text"))
            :on-click #(c/play-track state)}
     (if (:track-chan (:player @state)) "pause" "play")] " "]])

(defn channel
  "One line of attributes for a single channel."
  [[note- gain effect] chan-id chan-active? state]
  (let [[note-- octave] note-
        note        (case note--
                      :off "X"
                      :stop "S"
                      nil  "-"
                      (note-name note--))
        octave      (or octave "-")
        attr-strs   [(str " " note (when (= 1 (count note)) "-") octave " ")
                     (if gain   (str " " gain " ")   " - ")
                     (if effect (str " " effect " ") " - ")]
        active-attr (if chan-active? (:active-attr @state) -1)
        mode-       (when chan-active? (:mode @state))]
    [:span.ps
     (for [[s attr-num] (map vector attr-strs (range))
           :let [attr-id (str chan-id "-" attr-num)
                 attr-active? (and chan-active?
                                   (= attr-num active-attr))
                 ;; spaghetti; changing this shit is gonna suck
                 mode (when (and attr-active? (= mode- :edit)) mode-)]]
       ^{:key attr-id}
       [:span {:id attr-id
               :class (str (name (if attr-active?
                                   (or mode :active-attr)
                                   :attr))
                           (when (= note-- :off)  " bright-text")
                           (when (= note-- :stop) " stopline"))}
        s])])) ; <-- it's part of the span

;; XXX this is horrible
(defn number->hex [n]
  (as-> n s
    (.toString s 16)
    (gs/format "%2s" s)
    (.toUpperCase s)
    (.join (.split s " ") "0")))

(defn line-hex [hex bright?]
  [:span [:span
          {:class (when bright? :bright-text)}
          (str " " hex " ")] ""])  ; savage

(defn frame-hex [line-number hex state]
  [:span.attr
   {:id (str line-number "-f")
    ;; af is inconsistent with active-attr FYI
    :class (str "ps"
                (when (= line-number (:active-frame @state)) " active-attr af")
                (when ((:used-frames @state) line-number) " bright-text"))}
   (str " " hex " ")])

(defn line
  "One line of channels"
  [slice line-id line-number line-active? state]
  (let [hex (number->hex line-number)
        bright? (zero? (mod line-number 4))]
    [:pre.slice {:id line-id}
     [:span      {:class (when line-active? :active-line)}
      [line-hex hex bright?]
      (let [active-chan (if line-active?
                          (:active-chan @state)
                          -1)]
        (for [[attrs chan-number] (map vector slice (range))
              :let [chan-active? (and line-active?
                                      (= chan-number active-chan))
                    chan-id (str line-number "-" chan-number)]]
          ^{:key chan-id}
          [channel attrs chan-id chan-active? state]))]
     [frame-hex line-number hex state]]))

(defn main-ui
  "Main UI. Combines scheme, track, controls, modeline, etc.
  `scheme` is a list of keywords defining the channel instruments and order
  `slices` is a vector of vectors containing note attributes (not a good name)
  `state` is the current uh... XXX I don't remember
  "
  [scheme slices state]
  [:div#main-ui
   [:div#tracker
    [scheme-line scheme]
    [:div#slices
     (let [active-line (:active-line @state)
           active-frame (:active-frame @state)
           frame (get (:slices @state) active-frame)]
       (for [[slice line-number] (map vector frame (range))
             :let [line-id (str "line-" line-number)]]
         ^{:key line-id}
         [line slice
          line-id
          line-number
          (= line-number active-line)
          state]))]
    [modeline
     (apply str
            [" " "bpm" (:bpm @state)
             " " "octave" (:octave @state)])
     (str "uh" " ")
     state]]])
