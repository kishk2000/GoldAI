package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.List;

public class PredictionEngine {

    private static final double THRESHOLD = 0.003;
    private static final int MIN_HISTORY = 15;
    private static final int LOOKBACK = 25;

    public static class PredictionResult {

        public double predictedPrice;
        public String direction;
        public double confidence;

        public PredictionResult(
                double predictedPrice,
                String direction,
                double confidence) {

            this.predictedPrice = predictedPrice;
            this.direction = direction;
            this.confidence = confidence;
        }
    }

    private static class State {

        double score;
        double returnValue;
        int direction;

        State(
                double score,
                double returnValue,
                int direction) {

            this.score = score;
            this.returnValue = returnValue;
            this.direction = direction;
        }
    }

    public PredictionResult analyze(
            double currentPrice,
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null ||
                bars.size() < MIN_HISTORY ||
                currentPrice <= 0) {

            return new PredictionResult(
                    currentPrice,
                    "محايد",
                    50
            );
        }

        int currentIndex =
                bars.size() - 1;

        double currentScore =
                calculateScore(
                        bars,
                        currentIndex
                );

        /*
         * =====================================================
         * الاحتمالات الفنية الحالية
         * =====================================================
         */

        double[] technicalProbabilities =
                technicalProbabilities(
                        currentScore
                );

        /*
         * =====================================================
         * بناء الحالات التاريخية
         * =====================================================
         */

        List<State> historicalStates =
                new ArrayList<>();

        int start =
                Math.max(
                        15,
                        currentIndex - LOOKBACK
                );

        for (
                int i = start;
                i < currentIndex;
                i++
        ) {

            if (i + 1 >= bars.size()) {
                break;
            }

            double score =
                    calculateScore(
                            bars,
                            i
                    );

            double today =
                    bars.get(i).close;

            double tomorrow =
                    bars.get(i + 1).close;

            if (today <= 0 ||
                    tomorrow <= 0) {
                continue;
            }

            double change =
                    (tomorrow - today)
                            / today;

            int direction;

            if (change >= THRESHOLD) {

                direction = 1;

            } else if (change <= -THRESHOLD) {

                direction = -1;

            } else {

                direction = 0;
            }

            historicalStates.add(
                    new State(
                            score,
                            change,
                            direction
                    )
            );
        }

        /*
         * =====================================================
         * الاحتمالات التاريخية
         * =====================================================
         */

        double[] historicalProbabilities =
                historicalProbabilities(
                        currentScore,
                        historicalStates
                );

        /*
         * =====================================================
         * دمج الفني + التاريخي
         * =====================================================
         */

        double upProbability =
                technicalProbabilities[0] * 0.35
                        + historicalProbabilities[0] * 0.65;

        double downProbability =
                technicalProbabilities[1] * 0.35
                        + historicalProbabilities[1] * 0.65;

        double sidewaysProbability =
                technicalProbabilities[2] * 0.35
                        + historicalProbabilities[2] * 0.65;

        /*
         * Normalize
         */

        double total =
                upProbability
                        + downProbability
                        + sidewaysProbability;

        if (total > 0) {

            upProbability /= total;
            downProbability /= total;
            sidewaysProbability /= total;
        }

        /*
         * =====================================================
         * تحديد الاتجاه
         * =====================================================
         */

        double maxProbability =
                Math.max(
                        upProbability,
                        Math.max(
                                downProbability,
                                sidewaysProbability
                        )
                );

        String direction;

        if (maxProbability == upProbability) {

            direction = "صاعد";

        } else if (maxProbability == downProbability) {

            direction = "هابط";

        } else {

            direction = "محايد";
        }

        /*
         * الاحتمال الثاني
         */

        double secondProbability =
                secondLargest(
                        upProbability,
                        downProbability,
                        sidewaysProbability
                );

        /*
         * إذا كانت المنافسة بين الاتجاهات قوية،
         * نعتبر السوق محايدًا.
         */

        if (
                maxProbability < 0.40 ||
                        maxProbability - secondProbability < 0.08
        ) {

            direction = "محايد";
        }

        /*
         * =====================================================
         * العائد المتوقع
         *
         * النسخة الأصلية:
         * متوسط مرجح بالحالات التاريخية
         * القريبة من الـ Score الحالي.
         * =====================================================
         */

        double expectedReturn =
                weightedHistoricalReturn(
                        currentScore,
                        historicalStates
                );

        /*
         * في حالة عدم وجود حالات مناسبة،
         * نستخدم الاحتمالات العامة.
         */

        if (historicalStates.isEmpty()) {

            expectedReturn =
                    (upProbability - downProbability)
                            * 0.006;
        }

        /*
         * تقليل المبالغة.
         */

        expectedReturn *= 0.65;

        /*
         * العرضي يكون أكثر تحفظًا.
         */

        if (direction.equals("محايد")) {

            expectedReturn *= 0.45;
        }

        /*
         * =====================================================
         * حماية من التوقعات المبالغ فيها
         * =====================================================
         */

        if (expectedReturn > 0.025) {
            expectedReturn = 0.025;
        }

        if (expectedReturn < -0.025) {
            expectedReturn = -0.025;
        }

        /*
         * =====================================================
         * السعر المتوقع
         * =====================================================
         */

        double predictedPrice =
                currentPrice
                        * (1.0 + expectedReturn);

        /*
         * =====================================================
         * الثقة
         * =====================================================
         */

        double confidence =
                50.0
                        + (maxProbability - 0.3333)
                        * 120.0
                        + (maxProbability - secondProbability)
                        * 60.0;

        if (confidence < 40) {
            confidence = 40;
        }

        if (confidence > 85) {
            confidence = 85;
        }

        return new PredictionResult(
                predictedPrice,
                direction,
                confidence
        );
    }

    /*
     * =========================================================
     * Technical Score
     * =========================================================
     */

    private double calculateScore(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index) {

        if (index < 15) {
            return 0;
        }

        double price =
                bars.get(index).close;

        double ema5 =
                ema(
                        bars,
                        index,
                        5
                );

        double ema10 =
                ema(
                        bars,
                        index,
                        10
                );

        double ema20 =
                ema(
                        bars,
                        index,
                        20
                );

        double rsi =
                calculateRSI(
                        bars,
                        index,
                        14
                );

        double momentum1 =
                momentum(
                        bars,
                        index,
                        1
                );

        double momentum3 =
                momentum(
                        bars,
                        index,
                        3
                );

        double momentum5 =
                momentum(
                        bars,
                        index,
                        5
                );

        double score = 0;

        /*
         * Trend
         */

        if (ema5 > ema10) {
            score += 1.5;
        } else {
            score -= 1.5;
        }

        if (ema10 > ema20) {
            score += 1.5;
        } else {
            score -= 1.5;
        }

        /*
         * Position
         */

        if (price > ema5) {
            score += 1.0;
        } else {
            score -= 1.0;
        }

        if (price > ema10) {
            score += 0.75;
        } else {
            score -= 0.75;
        }

        /*
         * Momentum
         */

        score += momentumSignal(
                momentum1,
                1.0
        );

        score += momentumSignal(
                momentum3,
                1.5
        );

        score += momentumSignal(
                momentum5,
                2.0
        );

        /*
         * RSI
         */

        if (rsi >= 55 && rsi <= 68) {

            score += 1.5;

        } else if (rsi >= 45 && rsi < 55) {

            score += 0;

        } else if (rsi >= 32 && rsi < 45) {

            score -= 1.0;

        } else if (rsi < 32) {

            score += 1.0;

        } else if (rsi > 68) {

            score -= 1.0;
        }

        return score;
    }

    private double momentumSignal(
            double momentum,
            double weight) {

        if (momentum > 0.003) {
            return weight;
        }

        if (momentum < -0.003) {
            return -weight;
        }

        double proportional =
                momentum / 0.003;

        if (proportional > 1) {
            proportional = 1;
        }

        if (proportional < -1) {
            proportional = -1;
        }

        return proportional * weight;
    }

    /*
     * =========================================================
     * Technical Probabilities
     * =========================================================
     */

    private double[] technicalProbabilities(
            double score) {

        double up =
                Math.exp(
                        score * 0.35
                );

        double down =
                Math.exp(
                        -score * 0.35
                );

        double sideways =
                Math.exp(
                        -Math.abs(score) * 0.20
                );

        double total =
                up + down + sideways;

        return new double[] {
                up / total,
                down / total,
                sideways / total
        };
    }

    /*
     * =========================================================
     * Historical Probabilities
     * =========================================================
     */

    private double[] historicalProbabilities(
            double currentScore,
            List<State> states) {

        if (states.isEmpty()) {

            return new double[] {
                    0.3333,
                    0.3333,
                    0.3334
            };
        }

        double up = 0;
        double down = 0;
        double sideways = 0;

        double totalWeight = 0;

        for (State state : states) {

            double distance =
                    Math.abs(
                            state.score
                                    - currentScore
                    );

            double weight =
                    1.0
                            / (1.0 + distance);

            if (state.direction > 0) {

                up += weight;

            } else if (state.direction < 0) {

                down += weight;

            } else {

                sideways += weight;
            }

            totalWeight += weight;
        }

        if (totalWeight <= 0) {

            return new double[] {
                    0.3333,
                    0.3333,
                    0.3334
            };
        }

        return new double[] {
                up / totalWeight,
                down / totalWeight,
                sideways / totalWeight
        };
    }

    /*
     * =========================================================
     * Weighted Historical Return
     * =========================================================
     */

    private double weightedHistoricalReturn(
            double currentScore,
            List<State> states) {

        if (states.isEmpty()) {
            return 0;
        }

        double weightedReturn = 0;
        double totalWeight = 0;

        for (State state : states) {

            double distance =
                    Math.abs(
                            state.score
                                    - currentScore
                    );

            double weight =
                    1.0
                            / (1.0 + distance);

            weightedReturn +=
                    state.returnValue
                            * weight;

            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return 0;
        }

        return weightedReturn
                / totalWeight;
    }

    /*
     * =========================================================
     * EMA
     * =========================================================
     */

    private double ema(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index,
            int period) {

        if (index < 0) {
            return bars.get(0).close;
        }

        int start =
                Math.max(
                        0,
                        index - period * 3
                );

        double ema =
                bars.get(start).close;

        double multiplier =
                2.0
                        / (period + 1.0);

        for (
                int i = start + 1;
                i <= index;
                i++
        ) {

            double price =
                    bars.get(i).close;

            ema =
                    (price - ema)
                            * multiplier
                            + ema;
        }

        return ema;
    }

    /*
     * =========================================================
     * RSI
     * =========================================================
     */

    private double calculateRSI(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index,
            int period) {

        if (index < period) {
            return 50;
        }

        double gain = 0;
        double loss = 0;

        int start =
                index - period + 1;

        for (
                int i = start;
                i <= index;
                i++
        ) {

            double change =
                    bars.get(i).close
                            - bars.get(i - 1).close;

            if (change > 0) {

                gain += change;

            } else {

                loss -= change;
            }
        }

        if (loss == 0) {
            return 100;
        }

        double averageGain =
                gain / period;

        double averageLoss =
                loss / period;

        if (averageLoss == 0) {
            return 100;
        }

        double rs =
                averageGain
                        / averageLoss;

        return 100
                - (100
                / (1 + rs));
    }

    /*
     * =========================================================
     * Momentum
     * =========================================================
     */

    private double momentum(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index,
            int period) {

        if (index < period) {
            return 0;
        }

        double current =
                bars.get(index).close;

        double previous =
                bars.get(index - period).close;

        if (previous == 0) {
            return 0;
        }

        return
                (current - previous)
                        / previous;
    }

    /*
     * =========================================================
     * Second Largest Probability
     * =========================================================
     */

    private double secondLargest(
            double a,
            double b,
            double c) {

        double largest =
                Math.max(
                        a,
                        Math.max(b, c)
                );

        if (a == largest) {

            return Math.max(b, c);

        } else if (b == largest) {

            return Math.max(a, c);

        } else {

            return Math.max(a, b);
        }
    }
}
