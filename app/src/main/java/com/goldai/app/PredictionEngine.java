package com.goldai.app;

public class PredictionEngine {

    private double previousGoldPrice = 0;

    public PredictionResult analyze(double currentGoldPrice) {

        if (previousGoldPrice == 0) {

            previousGoldPrice = currentGoldPrice;

            return new PredictionResult(
                    currentGoldPrice,
                    currentGoldPrice,
                    "مستقر",
                    50
            );
        }

        double change =
                currentGoldPrice - previousGoldPrice;

        double predictedPrice =
                currentGoldPrice + (change * 2.0);

        double difference =
                Math.abs(predictedPrice - currentGoldPrice);

        double confidence = 50;

        if (difference > 2) {
            confidence = 60;
        }

        if (difference > 5) {
            confidence = 65;
        }

        String direction;

        if (change > 0.05) {

            direction = "صعود ↑";

        } else if (change < -0.05) {

            direction = "هبوط ↓";

        } else {

            direction = "مستقر";
        }

        previousGoldPrice =
                currentGoldPrice;

        return new PredictionResult(
                currentGoldPrice,
                predictedPrice,
                direction,
                confidence
        );
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

            this.currentPrice = currentPrice;
            this.predictedPrice = predictedPrice;
            this.direction = direction;
            this.confidence = confidence;
        }
    }
}
