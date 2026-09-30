package com.goldai.app;

import java.util.ArrayList;
import java.util.List;

public class PredictionEngine {

    private static final double THRESHOLD = 0.003;
    private static final int MIN_HISTORY = 15;
    private static final int LOOKBACK = 25;

    public static class PredictionResult {

        public double predictedPrice;
        public String direction;
        public int confidence;

        public PredictionResult(
                double predictedPrice,
                String direction,
                int confidence) {

            this.predictedPrice = predictedPrice;
            this.direction = direction;
            this.confidence = confidence;
        }
    }

    private static class State {

        double score;
        double returnValue;
        String direction;

        State(
                double score,
                double returnValue,
                String direction) {

            this.score = score;
            this.returnValue = returnValue;
            this.direction = direction;
        }
    }

    public PredictionResult analyze(
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null || bars.size() < MIN_HISTORY) {

            return new PredictionResult(
                    0,
                    "محايد",
                    40
            );
        }

        int currentIndex = bars.size() - 1;

        double currentPrice =
                bars.get(currentIndex).close;

        if (currentPrice <= 0) {

            return new PredictionResult(
                    0,
                    "محايد",
                    40
            );
        }

        /*
         * ==========================================
         * 1. Current technical score
         * ==========================================
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

        double ensembleScore =
                trendScore
                        + positionScore
                        + momentumScore
                        + rsiScore;


        /*
         * ==========================================
         * 2. Current indicators
         * ==========================================
         */

        double ema5 =
                ema(
                        bars,
                        currentIndex,
                        5
                );

        double ema10 =
                ema(
                        bars,
                        currentIndex,
                        10
                );

        double ema20 =
                ema(
                        bars,
                        currentIndex,
                        20
                );

        double rsi =
                calculateRSI(
                        bars,
                        currentIndex,
                        14
                );

        double momentum1 =
                momentum(
                        bars,
                        currentIndex,
                        1
                );

        double momentum3 =
                momentum(
                        bars,
                        currentIndex,
                        3
                );

        double momentum5 =
                momentum(
                        bars,
                        currentIndex,
                        5
                );


        /*
         * ==========================================
         * 3. Historical states
         * ==========================================
         */

        List<State> historicalStates =
                new ArrayList<>();

        int start =
                Math.max(
                        MIN_HISTORY,
                        currentIndex - LOOKBACK
                );

        for (int i = start; i < currentIndex; i++) {

            double score =
                    calculateScore(
                            bars,
                            i
                    );

            double today =
                    bars.get(i).close;

            double tomorrow =
                    bars.get(i + 1).close;

            double change =
                    (tomorrow - today)
                            / today;

            String direction;

            if (change >= THRESHOLD) {

                direction = "صاعد";

            } else if (change <= -THRESHOLD) {

                direction = "هابط";

            } else {

                direction = "محايد";
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
         * ==========================================
         * 4. Historical expected return
         * ==========================================
         */

        double historicalReturn =
                weightedHistoricalReturn(
                        ensembleScore,
                        historicalStates
                );


        /*
         * ==========================================
         * 5. Technical expected return
         *
         * هنا بنفصل حجم الحركة عن الاتجاه.
         * ==========================================
         */

        double trendReturn = 0.0;

        if (ema20 > 0) {

            trendReturn =
                    (ema5 - ema20)
                            / ema20;
        }

        /*
         * Momentum contribution.
         *
         * 1D أقل وزنًا لأنه سريع جدًا.
         * 5D أكبر لأنه يعبر عن الاتجاه الممتد.
         */

        double technicalReturn =
                momentum1 * 0.15
                        + momentum3 * 0.25
                        + momentum5 * 0.30
                        + trendReturn * 0.20;


        /*
         * RSI adjustment.
         */

        if (rsi < 32) {

            technicalReturn += 0.002;

        } else if (rsi > 68) {

            technicalReturn -= 0.002;

        } else if (rsi >= 55 && rsi <= 68) {

            technicalReturn += 0.001;

        } else if (rsi >= 32 && rsi < 45) {

            technicalReturn -= 0.001;
        }


        /*
         * Momentum acceleration.
         */

        double momentumAcceleration =
                (
                        momentum1
                                - momentum5 / 5.0
                ) * 0.15;

        technicalReturn +=
                momentumAcceleration;


        /*
         * ==========================================
         * 6. Vote strength
         * ==========================================
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
         * عندما المؤشرات مش متفقة،
         * نقلل حجم التوقع بدل ما نغيّر الاتجاه عشوائيًا.
         */

        if (voteDifference <= 1) {

            technicalReturn *= 0.60;

        } else if (voteDifference == 2) {

            technicalReturn *= 0.85;

        } else {

            technicalReturn *= 1.05;
        }


        /*
         * ==========================================
         * 7. Blend historical + technical
         * ==========================================
         *
         * بدل الاعتماد على التاريخ وحده،
         * نستخدم الاثنين معًا.
         */

        double expectedReturn =
                historicalReturn * 0.50
                        + technicalReturn * 0.50;


        /*
         * منع التوقعات المبالغ فيها.
         */

        expectedReturn *= 0.45;

        if (expectedReturn > 0.03) {
            expectedReturn = 0.03;
        }

        if (expectedReturn < -0.03) {
            expectedReturn = -0.03;
        }


        /*
         * ==========================================
         * 8. Direction
         * ==========================================
         */

        String direction;

        if (
                ensembleScore >= 3
                        && expectedReturn >= THRESHOLD
        ) {

            direction = "صاعد";

        } else if (
                ensembleScore <= -3
                        && expectedReturn <= -THRESHOLD
        ) {

            direction = "هابط";

        } else if (ensembleScore >= 4) {

            direction = "صاعد";

        } else if (ensembleScore <= -4) {

            direction = "هابط";

        } else {

            direction = "محايد";
        }


        /*
         * لو الاتجاه محايد، نقلل حجم الحركة.
         */

        if (direction.equals("محايد")) {

            expectedReturn *= 0.45;
        }


        /*
         * ==========================================
         * 9. Predicted price
         * ==========================================
         */

        double predictedPrice =
                currentPrice
                        * (1.0 + expectedReturn);


        /*
         * ==========================================
         * 10. Confidence
         * ==========================================
         */

        double scoreStrength =
                Math.min(
                        1.0,
                        Math.abs(ensembleScore) / 6.0
                );

        double voteStrength =
                voteDifference / 4.0;

        double movementStrength =
                Math.min(
                        1.0,
                        Math.abs(expectedReturn) / 0.01
                );

        double rsiStrength = 0.0;

        if (rsi < 30 || rsi > 70) {
            rsiStrength = 0.15;
        }

        double confidence =
                45.0
                        + scoreStrength * 25.0
                        + voteStrength * 10.0
                        + movementStrength * 10.0
                        + rsiStrength * 10.0;

        /*
         * لا نريد ثقة 85% بسهولة.
         */

        if (confidence > 85) {
            confidence = 85;
        }

        if (confidence < 40) {
            confidence = 40;
        }


        return new PredictionResult(
                predictedPrice,
                direction,
                (int) Math.round(confidence)
        );
    }


    /*
     * ==========================================
     * Combined score
     * ==========================================
     */

    private double calculateScore(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index) {

        double trendScore =
                calculateTrendScore(
                        bars,
                        index
                );

        double positionScore =
                calculatePositionScore(
                        bars,
                        index
                );

        double momentumScore =
                calculateMomentumScore(
                        bars,
                        index
                );

        double rsiScore =
                calculateRsiScore(
                        bars,
                        index
                );

        return trendScore
                + positionScore
                + momentumScore
                + rsiScore;
    }


    /*
     * ==========================================
     * Trend
     * ==========================================
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

        double score = 0.0;

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
     * ==========================================
     * Position
     * ==========================================
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

        double score = 0.0;

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
     * ==========================================
     * Momentum
     * ==========================================
     */

    private double calculateMomentumScore(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index) {

        double m1 =
                momentum(
                        bars,
                        index,
                        1
                );

        double m3 =
                momentum(
                        bars,
                        index,
                        3
                );

        double m5 =
                momentum(
                        bars,
                        index,
                        5
                );

        double score = 0.0;

        score += momentumSignal(m1, 1.0);
        score += momentumSignal(m3, 1.5);
        score += momentumSignal(m5, 2.0);

        return score;
    }


    private double momentumSignal(
            double value,
            double weight) {

        if (value >= THRESHOLD) {

            return weight;

        } else if (value <= -THRESHOLD) {

            return -weight;

        } else {

            return
                    (value / THRESHOLD)
                            * weight;
        }
    }


    /*
     * ==========================================
     * RSI
     * ==========================================
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

            return 0.0;

        } else if (rsi >= 32 && rsi < 45) {

            return -1.0;

        } else if (rsi < 32) {

            return 1.0;

        } else {

            return -1.0;
        }
    }


    /*
     * ==========================================
     * Historical weighted return
     * ==========================================
     */

    private double weightedHistoricalReturn(
            double currentScore,
            List<State> states) {

        if (
                states == null
                        || states.isEmpty()
        ) {

            return 0.0;
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (State state : states) {

            double difference =
                    Math.abs(
                            state.score
                                    - currentScore
                    );

            double weight =
                    1.0
                            / (1.0 + difference);

            weightedSum +=
                    state.returnValue
                            * weight;

            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return 0.0;
        }

        return weightedSum
                / totalWeight;
    }


    /*
     * ==========================================
     * EMA
     * ==========================================
     */

    private double ema(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index,
            int period) {

        if (index < 0) {
            return 0.0;
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

        for (int i = start + 1;
             i <= index;
             i++) {

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
     * ==========================================
     * RSI
     * ==========================================
     */

    private double calculateRSI(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index,
            int period) {

        if (index < period) {
            return 50.0;
        }

        double gains = 0.0;
        double losses = 0.0;

        int start =
                index - period + 1;

        for (int i = start; i <= index; i++) {

            double change =
                    bars.get(i).close
                            - bars.get(i - 1).close;

            if (change > 0) {

                gains += change;

            } else {

                losses -= change;
            }
        }

        if (losses == 0) {

            return 100.0;
        }

        double averageGain =
                gains / period;

        double averageLoss =
                losses / period;

        if (averageLoss == 0) {
            return 100.0;
        }

        double rs =
                averageGain
                        / averageLoss;

        return
                100.0
                        - (
                        100.0
                                / (1.0 + rs)
                );
    }


    /*
     * ==========================================
     * Momentum
     * ==========================================
     */

    private double momentum(
            List<HistoricalGoldProvider.GoldBar> bars,
            int index,
            int days) {

        if (index < days) {
            return 0.0;
        }

        double current =
                bars.get(index).close;

        double previous =
                bars.get(index - days).close;

        if (previous == 0) {
            return 0.0;
        }

        return
                (current - previous)
                        / previous;
    }
}
