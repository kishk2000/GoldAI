package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BacktestEngine {

    private final PredictionEngine predictionEngine =
            new PredictionEngine();

    public BacktestResult run(
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null || bars.size() < 7) {

            return new BacktestResult(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        List<HistoricalGoldProvider.GoldBar> chronological =
                new ArrayList<>(bars);

        Collections.sort(
                chronological,
                (a, b) ->
                        a.date.compareTo(b.date)
        );

        int totalTests = 0;
        int correctTests = 0;

        double totalAbsoluteError = 0;

        int predictedUp = 0;
        int predictedDown = 0;
        int predictedSideways = 0;

        int correctUp = 0;
        int correctDown = 0;

        for (
                int i = 6;
                i < chronological.size() - 1;
                i++
        ) {

            List<HistoricalGoldProvider.GoldBar> training =
                    new ArrayList<>(
                            chronological.subList(
                                    0,
                                    i + 1
                            )
                    );

            double currentPrice =
                    chronological
                            .get(i)
                            .close;

            double actualNextPrice =
                    chronological
                            .get(i + 1)
                            .close;

            PredictionEngine.PredictionResult result =
                    predictionEngine.analyze(
                            currentPrice,
                            training
                    );

            double predictedPrice =
                    result.predictedPrice;

            boolean actualUp =
                    actualNextPrice > currentPrice;

            boolean actualDown =
                    actualNextPrice < currentPrice;

            boolean predictedUp =
                    result.direction.contains(
                            "صعود"
                    );

            boolean predictedDown =
                    result.direction.contains(
                            "هبوط"
                    );

            boolean predictedSideways =
                    result.direction.contains(
                            "عرضي"
                    );

            /*
             * عدد توقعات كل اتجاه
             */

            if (predictedUp) {

                predictedUp++;

            } else if (predictedDown) {

                predictedDown++;

            } else if (predictedSideways) {

                predictedSideways++;
            }

            /*
             * دقة الاتجاه
             */

            if (actualUp && predictedUp) {

                correctTests++;
                correctUp++;

            } else if (actualDown && predictedDown) {

                correctTests++;
                correctDown++;

            } else if (
                    Math.abs(
                            actualNextPrice
                                    - currentPrice
                    ) < 0.01
            ) {

                correctTests++;
            }

            /*
             * خطأ السعر
             */

            double absoluteError =
                    Math.abs(
                            predictedPrice
                                    - actualNextPrice
                    );

            totalAbsoluteError +=
                    absoluteError;

            totalTests++;
        }

        if (totalTests == 0) {

            return new BacktestResult(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        double directionAccuracy =
                (correctTests * 100.0)
                        / totalTests;

        double averageAbsoluteError =
                totalAbsoluteError
                        / totalTests;

        double upAccuracy =
                predictedUp > 0
                        ? (correctUp * 100.0)
                        / predictedUp
                        : 0;

        double downAccuracy =
                predictedDown > 0
                        ? (correctDown * 100.0)
                        / predictedDown
                        : 0;

        return new BacktestResult(
                totalTests,
                correctTests,
                directionAccuracy,
                averageAbsoluteError,
                predictedUp,
                predictedDown,
                predictedSideways,
                upAccuracy,
                downAccuracy
        );
    }

    public static class BacktestResult {

        public int totalTests;

        public int correctTests;

        public double directionAccuracy;

        public double averageAbsoluteError;

        public int predictedUp;

        public int predictedDown;

        public int predictedSideways;

        public double upAccuracy;

        public double downAccuracy;

        public BacktestResult(
                int totalTests,
                int correctTests,
                double directionAccuracy,
                double averageAbsoluteError,
                int predictedUp,
                int predictedDown,
                int predictedSideways,
                double upAccuracy,
                double downAccuracy) {

            this.totalTests =
                    totalTests;

            this.correctTests =
                    correctTests;

            this.directionAccuracy =
                    directionAccuracy;

            this.averageAbsoluteError =
                    averageAbsoluteError;

            this.predictedUp =
                    predictedUp;

            this.predictedDown =
                    predictedDown;

            this.predictedSideways =
                    predictedSideways;

            this.upAccuracy =
                    upAccuracy;

            this.downAccuracy =
                    downAccuracy;
        }
    }
}
