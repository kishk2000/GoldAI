package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

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

        double sma5 = calculateSMA(bars, 5);

        double sma10 =
                calculateSMA(
                        bars,
                        Math.min(10, bars.size())
                );

        double previousClose =
                bars.get(0).close;

        double change =
                currentPrice - previousClose;

        double changePercent =
                (change / previousClose) * 100.0;

        String direction;

        if (currentPrice > sma5 &&
                sma5 > sma10) {

            direction = "صعود ↑";

        } else if (currentPrice < sma5 &&
                sma5 < sma10) {

            direction = "هبوط ↓";

        } else {

            direction = "عرضي ↔";
        }

        double momentum =
                calculateMomentum(bars);

        double predictedPrice =
                currentPrice
                        * (1.0 + momentum);

        double volatility =
                calculateVolatility(bars);

        double confidence =
                calculateConfidence(
                        changePercent,
                        volatility,
                        currentPrice,
                        sma5,
                        sma10
                );

        return new PredictionResult(
                currentPrice,
                predictedPrice,
                direction,
                confidence
        );
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

        for (int i = 0; i < count; i++) {

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
                bars.get(0).close;

        double previous =
                bars.get(1).close;

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

        double average = 0;

        for (int i = 0; i < count; i++) {

            average += bars.get(i).close;
        }

        average /= count;

        double variance = 0;

        for (int i = 0; i < count; i++) {

            double difference =
                    bars.get(i).close - average;

            variance +=
                    difference * difference;
        }

        variance /= count;

        return Math.sqrt(variance) / average;
    }

    private double calculateConfidence(
            double changePercent,
            double volatility,
            double currentPrice,
            double sma5,
            double sma10) {

        double confidence = 50;

        if (currentPrice > sma5 &&
                sma5 > sma10) {

            confidence += 10;
        }

        if (currentPrice < sma5 &&
                sma5 < sma10) {

            confidence += 10;
        }

        if (Math.abs(changePercent) > 1) {

            confidence += 5;
        }

        if (volatility < 0.03) {

            confidence += 5;
        }

        if (volatility > 0.06) {

            confidence -= 10;
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
