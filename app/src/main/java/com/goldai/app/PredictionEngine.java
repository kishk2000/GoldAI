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
         * حساب مكونات الاتجاه
         * =====================================================
         */

        double trendScore =
                calculateTrendScore(
                        bars,
                        currentIndex
                );

        double positionScore =
                calculatePositionScore(
                        bars,
                        currentIndex
                );

        double momentumScore =
                calculateMomentumScore(
                        bars,
                        currentIndex
                );

        double rsiScore =
                calculateRsiScore(
                        bars,
                        currentIndex
                );

        /*
         * =====================================================
         * Ensemble Score
         * =====================================================
         */

        double ensembleScore =
                trendScore
                        + positionScore
                        + momentumScore
                        + rsiScore;

        /*
         * =====================================================
         * توقع العائد
         *
         * نحتفظ بنفس طريقة Probability Ensemble
         * لحساب السعر المتوقع.
         * =====================================================
         */

        double expectedReturn =
                weightedHistoricalReturn(
                        currentScore,
                        historicalStates
                );

        if (historicalStates.isEmpty()) {

            expectedReturn =
                    0;
        }

        /*
         * تقليل المبالغة
         */

        expectedReturn *= 0.65;

        /*
         * =====================================================
         * حساب أصوات الاتجاه
         * =====================================================
         */

        int bullishVotes = 0;
        int bearishVotes = 0;

        if (trendScore > 0) {
            bullishVotes++;
        } else if (trendScore < 0) {
            bearishVotes++;
        }

        if (positionScore > 0) {
            bullishVotes++;
        } else if (positionScore < 0) {
            bearishVotes++;
        }

        if (momentumScore > 0) {
            bullishVotes++;
        } else if (momentumScore < 0) {
            bearishVotes++;
        }

        if (rsiScore > 0) {
            bullishVotes++;
        } else if (rsiScore < 0) {
            bearishVotes++;
        }

        int voteDifference =
                Math.abs(
                        bullishVotes
                                - bearishVotes
                );

        /*
         * =====================================================
         * ضبط حجم التوقع حسب اتفاق المؤشرات
         * =====================================================
         */

        if (voteDifference <= 1) {

            expectedReturn *= 0.60;

        } else if (voteDifference == 2) {

            expectedReturn *= 0.85;

        } else {

            expectedReturn *= 1.05;
        }

        /*
         * Calibration
         */

        expectedReturn *= 0.45;

        /*
         * حماية
         */

        if (expectedReturn > 0.03) {
            expectedReturn = 0.03;
        }

        if (expectedReturn < -0.03) {
            expectedReturn = -0.03;
        }

        /*
         * =====================================================
         * تحديد الاتجاه
         *
         * هنا التغيير الأساسي.
         * =====================================================
         */

        String direction;

        if (
                ensembleScore >= 3.0 &&
                        expectedReturn >= THRESHOLD
        ) {

            direction = "صاعد";

        } else if (
                ensembleScore <= -3.0 &&
                        expectedReturn <= -THRESHOLD
        ) {

            direction = "هابط";

        } else if (ensembleScore >= 4.0) {

            direction = "صاعد";

        } else if (ensembleScore <= -4.0) {

            direction = "هابط";

        } else {

            direction = "محايد";
        }

        /*
         * =====================================================
         * العرضي أكثر تحفظًا
         * =====================================================
         */

        if (direction.equals("محايد")) {

            expectedReturn *= 0.45;
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

        double scoreStrength =
                Math.min(
                        Math.abs(ensembleScore),
                        8.0
                );

        double voteStrength =
                voteDifference / 4.0;

        double movementStrength =
                Math.min(
                        Math.abs(expectedReturn) / 0.01,
                        1.0
                );

        double confidence =
                50.0
                        + scoreStrength * 3.5
                        + voteStrength * 12.0
                        + movementStrength * 8.0;

        /*
         * RSI extreme يعطي بعض الدعم للثقة
         */

        double rsi =
                calculateRSI(
                        bars,
                        currentIndex,
                        14
                );

        if (rsi < 32 || rsi > 68) {
            confidence += 3.0;
        }

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
     * Trend Score
     * =========================================================
     */

    private double calculateTrendScore(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index) {

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

        double score = 0;

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

        return score;
    }

    /*
     * =========================================================
     * Position Score
     * =========================================================
     */

    private double calculatePositionScore(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index) {

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

        double score = 0;

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

        return score;
    }

    /*
     * =========================================================
     * Momentum Score
     * =========================================================
     */

    private double calculateMomentumScore(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index) {

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

        return score;
    }

    /*
     * =========================================================
     * RSI Score
     * =========================================================
     */

    private double calculateRsiScore(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index) {

        double rsi =
                calculateRSI(
                        bars,
                        index,
                        14
                );

        if (rsi >= 55 && rsi <= 68) {

            return 1.5;

        } else if (rsi >= 45 && rsi < 55) {

            return 0;

        } else if (rsi >= 32 && rsi < 45) {

            return -1.0;

        } else if (rsi < 32) {

            return 1.0;

        } else {

            return -1.0;
        }
    }

    /*
     * =========================================================
     * Momentum Signal
     * =========================================================
     */

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
}
