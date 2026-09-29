package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredictionEngine {

    public PredictionResult analyze(
            double currentPrice,
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null || bars.size() < 5) {

            return new PredictionResult(
                    currentPrice,
                    currentPrice,
                    "بيانات غير كافية",
                    0
            );
        }

        // ترتيب البيانات: الأقدم ← الأحدث
        List<HistoricalGoldProvider.GoldBar> chronological =
                new ArrayList<>(bars);

        Collections.reverse(chronological);

        double sma5 =
                calculateSMA(
                        chronological,
                        5
                );

        double sma10 =
                calculateSMA(
                        chronological,
                        Math.min(
                                10,
                                chronological.size()
                        )
                );

        double rsi =
                calculateRSI(
                        chronological,
                        Math.min(
                                14,
                                chronological.size() - 1
                        )
                );

        double momentum =
                calculateMomentum(
                        chronological
                );

        double volatility =
                calculateVolatility(
                        chronological
                );

        String direction =
                determineDirection(
                        currentPrice,
                        sma5,
                        sma10,
                        rsi,
                        momentum
                );

        /*
         * التوقع الأساسي يعتمد على الزخم.
         * RSI يستخدم لتعديل قوة التوقع وليس كضمان للاتجاه.
         */
        double predictionFactor =
                momentum;

        if (rsi > 70) {

            // منطقة تشبع شرائي
            predictionFactor *= 0.50;

        } else if (rsi < 30) {

            // منطقة تشبع بيعي
            predictionFactor *= 0.50;
        }

        double predictedPrice =
                currentPrice *
                        (1.0 + predictionFactor);

        double confidence =
                calculateConfidence(
                        currentPrice,
                        sma5,
                        sma10,
                        rsi,
                        momentum,
                        volatility
                );

        return new PredictionResult(
                currentPrice,
                predictedPrice,
                direction,
                confidence
        );
    }

    private String determineDirection(
            double currentPrice,
            double sma5,
            double sma10,
            double rsi,
            double momentum) {

        int score = 0;

        // الاتجاه العام
        if (currentPrice > sma5) {
            score++;
        } else {
            score--;
        }

        if (sma5 > sma10) {
            score++;
        } else {
            score--;
        }

        // الزخم
        if (momentum > 0) {
            score++;
        } else if (momentum < 0) {
            score--;
        }

        // RSI
        if (rsi > 55 && rsi < 70) {
            score++;
        } else if (rsi < 45 && rsi > 30) {
            score--;
        }

        // تشبع قوي
        if (rsi >= 70) {
            score--;
        }

        if (rsi <= 30) {
            score++;
        }

        if (score >= 2) {
            return "صعود ↑";
        }

        if (score <= -2) {
            return "هبوط ↓";
        }

        return "عرضي ↔";
    }

    private double calculateSMA(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        int count =
                Math.min(
                        period,
                        bars.size()
                );

        double sum = 0;

        int start =
                bars.size() - count;

        for (int i = start;
             i < bars.size();
             i++) {

            sum += bars.get(i).close;
        }

        return sum / count;
    }

    private double calculateMomentum(
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars.size() < 2) {
            return 0;
        }

        double latest =
                bars.get(
                        bars.size() - 1
                ).close;

        double previous =
                bars.get(
                        bars.size() - 2
                ).close;

        return
                (latest - previous)
                        / previous;
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

        if (period <= 0) {
            return 50;
        }

        double gains = 0;
        double losses = 0;

        int start =
                bars.size() - period;

        for (int i = start;
             i < bars.size();
             i++) {

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
            return 100;
        }

        double relativeStrength =
                averageGain / averageLoss;

        double rsi =
                100 -
                        (100 /
                                (1 + relativeStrength));

        return rsi;
    }

    private double calculateVolatility(
            List<HistoricalGoldProvider.GoldBar> bars) {

        int count =
                Math.min(
                        10,
                        bars.size()
                );

        if (count == 0) {
            return 0;
        }

        double average = 0;

        int start =
                bars.size() - count;

        for (int i = start;
             i < bars.size();
             i++) {

            average +=
                    bars.get(i).close;
        }

        average /= count;

        double variance = 0;

        for (int i = start;
             i < bars.size();
             i++) {

            double difference =
                    bars.get(i).close
                            - average;

            variance +=
                    difference * difference;
        }

        variance /= count;

        return Math.sqrt(variance)
                / average;
    }

    private double calculateConfidence(
            double currentPrice,
            double sma5,
            double sma10,
            double rsi,
            double momentum,
            double volatility) {

        double confidence = 50;

        // توافق الاتجاه
        if (currentPrice > sma5 &&
                sma5 > sma10) {

            confidence += 10;

        } else if (
                currentPrice < sma5 &&
                        sma5 < sma10) {

            confidence += 10;
        }

        // زخم واضح
        if (Math.abs(momentum) > 0.005) {
            confidence += 5;
        }

        // RSI في منطقة متوسطة
        if (rsi >= 40 && rsi <= 60) {
            confidence += 5;
        }

        // تقليل الثقة مع التقلب العالي
        if (volatility > 0.06) {
            confidence -= 10;
        }

        if (volatility > 0.10) {
            confidence -= 10;
        }

        // عدم إعطاء ثقة مبالغ فيها
        if (confidence > 80) {
            confidence = 80;
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
