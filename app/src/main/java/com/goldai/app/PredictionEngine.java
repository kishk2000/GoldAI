package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredictionEngine {

    public PredictionResult analyze(
            double currentPrice,
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null || bars.size() < 6) {

            return new PredictionResult(
                    currentPrice,
                    currentPrice,
                    "بيانات غير كافية",
                    0
            );
        }

        // تحويل البيانات إلى: الأقدم ← الأحدث
        List<HistoricalGoldProvider.GoldBar> data =
                new ArrayList<>(bars);

        Collections.reverse(data);

        double ema5 =
                calculateEMA(data, 5);

        double ema10 =
                calculateEMA(data, 10);

        double rsi =
                calculateRSI(data, 14);

        double momentum =
                calculateMomentum(data);

        double volatility =
                calculateVolatility(data);

        // نظام النقاط
        int score = 0;

        // 1 — EMA
        if (ema5 > ema10) {
            score += 2;
        } else if (ema5 < ema10) {
            score -= 2;
        }

        // 2 — السعر بالنسبة للـ EMA
        if (currentPrice > ema5) {
            score += 1;
        } else if (currentPrice < ema5) {
            score -= 1;
        }

        // 3 — RSI
        if (rsi >= 55 && rsi < 70) {
            score += 2;
        } else if (rsi > 30 && rsi <= 45) {
            score -= 2;
        } else if (rsi >= 70) {
            // تشبع شرائي
            score -= 1;
        } else if (rsi <= 30) {
            // تشبع بيعي
            score += 1;
        }

        // 4 — Momentum
        if (momentum > 0.003) {
            score += 2;
        } else if (momentum < -0.003) {
            score -= 2;
        }

        // تحديد الاتجاه
        String direction;

        if (score >= 3) {

            direction = "صعود ↑";

        } else if (score <= -3) {

            direction = "هبوط ↓";

        } else {

            direction = "عرضي ↔";
        }

        /*
         * التوقع السعري:
         * نستخدم الزخم فقط كأساس للتغير،
         * ونحد التغير حتى لا ينتج المحرك
         * توقعات بعيدة جدًا عن السعر الحالي.
         */

        double adjustedMomentum =
                momentum;

        // تقليل تأثير الحركة عند التقلب العالي
        if (volatility > 0.05) {
            adjustedMomentum *= 0.70;
        }

        if (volatility > 0.08) {
            adjustedMomentum *= 0.50;
        }

        // منع التوقع من التحرك أكثر من 2% في الاختبار اليومي
        if (adjustedMomentum > 0.02) {
            adjustedMomentum = 0.02;
        }

        if (adjustedMomentum < -0.02) {
            adjustedMomentum = -0.02;
        }

        double predictedPrice =
                currentPrice *
                        (1.0 + adjustedMomentum);

        double confidence =
                calculateConfidence(
                        score,
                        rsi,
                        volatility,
                        momentum
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

        if (bars == null ||
                bars.isEmpty()) {

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

        for (int i = start + 1;
             i < bars.size();
             i++) {

            double close =
                    bars.get(i).close;

            ema =
                    (close - ema)
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

            if (averageGain == 0) {
                return 50;
            }

            return 100;
        }

        double rs =
                averageGain / averageLoss;

        return 100 -
                (100 / (1 + rs));
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

        if (previous == 0) {
            return 0;
        }

        return
                (latest - previous)
                        / previous;
    }

    private double calculateVolatility(
            List<HistoricalGoldProvider.GoldBar> bars) {

        int count =
                Math.min(
                        10,
                        bars.size()
                );

        if (count < 2) {
            return 0;
        }

        int start =
                bars.size() - count;

        double average = 0;

        for (int i = start;
             i < bars.size();
             i++) {

            average +=
                    bars.get(i).close;
        }

        average /= count;

        if (average == 0) {
            return 0;
        }

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

        return
                Math.sqrt(variance)
                        / average;
    }

    private double calculateConfidence(
            int score,
            double rsi,
            double volatility,
            double momentum) {

        double confidence = 50;

        // قوة اتفاق المؤشرات
        if (Math.abs(score) >= 5) {

            confidence += 15;

        } else if (Math.abs(score) >= 3) {

            confidence += 8;

        } else if (Math.abs(score) <= 1) {

            confidence -= 5;
        }

        // الزخم الواضح
        if (Math.abs(momentum) >= 0.005) {
            confidence += 5;
        }

        // التقلب العالي يقلل الثقة
        if (volatility > 0.05) {
            confidence -= 5;
        }

        if (volatility > 0.08) {
            confidence -= 10;
        }

        // RSI شديد التطرف = عدم يقين أكبر
        if (rsi >= 75 || rsi <= 25) {
            confidence -= 5;
        }

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
