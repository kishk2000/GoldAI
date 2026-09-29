package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredictionEngine {

    public PredictionResult analyze(
            double currentPrice,
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null || bars.size() < 15) {
            return new PredictionResult(
                    currentPrice,
                    currentPrice,
                    "بيانات غير كافية",
                    0
            );
        }

        List<HistoricalGoldProvider.GoldBar> data =
                new ArrayList<>(bars);

        Collections.sort(
                data,
                (a, b) -> a.date.compareTo(b.date)
        );

        double ema5 = calculateEMA(data, 5);
        double ema10 = calculateEMA(data, 10);
        double ema20 = calculateEMA(data, 20);
        double rsi = calculateRSI(data, 14);

        double momentum1 = calculateMomentum(data, 1);
        double momentum3 = calculateMomentum(data, 3);
        double momentum5 = calculateMomentum(data, 5);

        double volatility = calculateVolatility(data, 14);

        double score = 0;

        if (ema5 > ema10) {
            score += 2.0;
        } else {
            score -= 2.0;
        }

        if (ema10 > ema20) {
            score += 2.0;
        } else {
            score -= 2.0;
        }

        if (currentPrice > ema5) {
            score += 1.0;
        } else {
            score -= 1.0;
        }

        if (rsi >= 55 && rsi < 70) {

            score += 1.5;

        } else if (rsi > 45 && rsi < 55) {

            score += 0;

        } else if (rsi > 30 && rsi <= 45) {

            score -= 1.5;

        } else if (rsi >= 70) {

            score -= 1.0;

        } else if (rsi <= 30) {

            score += 1.0;
        }

        score += momentumScore(
                momentum1,
                2.0
        );

        score += momentumScore(
                momentum3,
                2.0
        );

        score += momentumScore(
                momentum5,
                2.0
        );

        String direction;

        if (score >= 3.0) {

            direction = "صعود ↑";

        } else if (score <= -3.0) {

            direction = "هبوط ↓";

        } else {

            direction = "عرضي ↔";
        }

        double forecastReturn =
                (
                        momentum1 * 0.20
                                +
                        momentum3 * 0.30
                                +
                        momentum5 * 0.35
                                +
                        trendReturn(
                                currentPrice,
                                ema10
                        ) * 0.15
                );

        if (rsi > 70) {

            forecastReturn -= 0.0025;

        } else if (rsi < 30) {

            forecastReturn += 0.0025;
        }

        double agreement =
                Math.abs(score);

        if (agreement >= 7) {

            forecastReturn *= 1.15;

        } else if (agreement <= 2) {

            forecastReturn *= 0.65;
        }

        if (volatility > 0.04) {

            forecastReturn *= 0.85;
        }

        if (volatility > 0.07) {

            forecastReturn *= 0.70;
        }

        if (forecastReturn > 0.03) {

            forecastReturn = 0.03;
        }

        if (forecastReturn < -0.03) {

            forecastReturn = -0.03;
        }

        double predictedPrice =
                currentPrice *
                        (1.0 + forecastReturn);

        double confidence =
                calculateConfidence(
                        score,
                        rsi,
                        volatility,
                        momentum1,
                        momentum3,
                        momentum5
                );

        return new PredictionResult(
                currentPrice,
                predictedPrice,
                direction,
                confidence
        );
    }

    private double calculateEMA(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        if (bars == null || bars.isEmpty()) {

            return 0;
        }

        int count =
                Math.min(
                        period,
                        bars.size()
                );

        int start =
                bars.size() - count;

        double ema =
                bars.get(start).close;

        double multiplier =
                2.0 / (count + 1.0);

        for (
                int i = start + 1;
                i < bars.size();
                i++
        ) {

            double close =
                    bars.get(i).close;

            ema =
                    (
                            close - ema
                    )
                            * multiplier
                            + ema;
        }

        return ema;
    }

    private double calculateRSI(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        if (bars.size() < 2) {

            return 50;
        }

        period =
                Math.min(
                        period,
                        bars.size() - 1
                );

        double gains = 0;
        double losses = 0;

        int start =
                bars.size() - period;

        for (
                int i = start;
                i < bars.size();
                i++
        ) {

            double current =
                    bars.get(i).close;

            double previous =
                    bars.get(i - 1).close;

            double change =
                    current - previous;

            if (change > 0) {

                gains += change;

            } else if (change < 0) {

                losses -= change;
            }
        }

        double averageGain =
                gains / period;

        double averageLoss =
                losses / period;

        if (averageLoss == 0) {

            if (averageGain == 0) {

                return 50;
            }

            return 100;
        }

        double rs =
                averageGain /
                        averageLoss;

        return 100 -
                (
                        100 /
                                (1 + rs)
                );
    }

    private double calculateMomentum(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        if (bars.size() <= period) {

            return 0;
        }

        int latestIndex =
                bars.size() - 1;

        double latest =
                bars.get(
                        latestIndex
                ).close;

        double previous =
                bars.get(
                        latestIndex - period
                ).close;

        if (previous == 0) {

            return 0;
        }

        return (
                latest - previous
        ) / previous;
    }

    private double trendReturn(
            double currentPrice,
            double ema) {

        if (ema == 0) {

            return 0;
        }

        return (
                currentPrice - ema
        ) / ema;
    }

    private double momentumScore(
            double momentum,
            double weight) {

        if (momentum > 0.003) {

            return weight;

        } else if (momentum < -0.003) {

            return -weight;
        }

        return 0;
    }

    private double calculateVolatility(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        int count =
                Math.min(
                        period,
                        bars.size()
                );

        if (count < 2) {

            return 0;
        }

        int start =
                bars.size() - count;

        double average = 0;

        for (
                int i = start;
                i < bars.size();
                i++
        ) {

            average +=
                    bars.get(i).close;
        }

        average /=
                count;

        if (average == 0) {

            return 0;
        }

        double variance = 0;

        for (
                int i = start;
                i < bars.size();
                i++
        ) {

            double difference =
                    bars.get(i).close
                            - average;

            variance +=
                    difference *
                            difference;
        }

        variance /=
                count;

        return Math.sqrt(
                variance
        ) / average;
    }

    private double calculateConfidence(
            double score,
            double rsi,
            double volatility,
            double momentum1,
            double momentum3,
            double momentum5) {

        double confidence = 50;

        if (Math.abs(score) >= 9) {

            confidence += 18;

        } else if (Math.abs(score) >= 7) {

            confidence += 13;

        } else if (Math.abs(score) >= 5) {

            confidence += 8;

        } else if (Math.abs(score) <= 2) {

            confidence -= 8;
        }

        boolean momentumAgreement =
                (
                        momentum1 > 0
                                &&
                        momentum3 > 0
                                &&
                        momentum5 > 0
                )
                        ||
                (
                        momentum1 < 0
                                &&
                        momentum3 < 0
                                &&
                        momentum5 < 0
                );

        if (momentumAgreement) {

            confidence += 7;
        }

        if (volatility > 0.04) {

            confidence -= 5;
        }

        if (volatility > 0.07) {

            confidence -= 10;
        }

        if (rsi >= 75 ||
                rsi <= 25) {

            confidence -= 5;
        }

        if (confidence > 85) {

            confidence = 85;
        }

        if (confidence < 20) {

            confidence = 20;
        }

        return confidence;
    }

    public static class PredictionResult {

        public double currentPrice;

        public double predictedPrice;

        public String direction;

        public double confidence;

        public PredictionResult(
                double currentPrice,
                double predictedPrice,
                String direction,
                double confidence) {

            this.currentPrice =
                    currentPrice;

            this.predictedPrice =
                    predictedPrice;

            this.direction =
                    direction;

            this.confidence =
                    confidence;
        }
    }
}
