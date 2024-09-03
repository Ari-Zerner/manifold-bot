(ns manifold-bot.strategies-test
  (:require [clojure.test :refer :all]
            [manifold-bot.strategies :refer :all]))

(defn approx
  "Approximately equal, for use in tests"
  ([x y]
   (approx x y 0.0001))
  ([x y epsilon]
   (< (Math/abs (- x y)) epsilon)))

(deftest kelly-bet-fraction-test
  ;; Test Case 1: Basic Positive Expected Value Bet
  (is (approx (kelly-bet-fraction 0.4 0.6) 0.3333))

  ;; Test Case 2: Full Deferral to Market (d = 1)
  (is (approx (kelly-bet-fraction 0.4 0.6 1) 0.0))

  ;; Test Case 3: Equal Weighting (d = 0.5)
  (is (approx (kelly-bet-fraction 0.4 0.6 0.5) 0.1667))

  ;; Test Case 4: Low Market Deferral (d = 0.2)
  (is (approx (kelly-bet-fraction 0.5 0.66 0.2) 0.256))

  ;; Test Case 5: High Market Deferral (d = 0.8)
  (is (approx (kelly-bet-fraction 0.3 0.38 0.8) 0.0229))

  ;; Test Case 6: Market Price Equals Initial Probability (Any Deferral)
  (is (approx (kelly-bet-fraction 0.5 0.5 0.5) 0.0)))

  ;; Edge Case 1: Probability of Winning is 0
  (is (approx (kelly-bet-fraction 0.2 0.0 0) 0.0))

  ;; Edge Case 2: Probability of Winning is 1
  (is (approx (kelly-bet-fraction 0.8 1.0 0) 1))

  ;; Edge Case 3: Market Price is 1 (No Payout)
  (is (approx (kelly-bet-fraction 1.0 0.75 0.5) 0.0)) ; Undefined Kelly fraction, should be 0

  ;; Edge Case 4: Deferral of 0 and Probability Matches Price
  (is (approx (kelly-bet-fraction 0.7 0.7 0) 0.0))

  ;; Edge Case 5: Market Deferral of 1 with High Initial Probability
  (is (approx (kelly-bet-fraction 0.3 0.3 1) 0.0))

  ;; Edge Case 6: Price Very Close to 0 (High Potential Payout)
  (is (approx (kelly-bet-fraction 0.01 0.05 0.5) 0.0202))

  ;; Edge Case 7: Extremely High Deferral and Market Price Close to 1
  (is (approx (kelly-bet-fraction 0.99 0.985 0.9) 0.0)) ; Negative Kelly fraction, should be 0
