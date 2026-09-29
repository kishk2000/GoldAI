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
                    0
            );
        }

        /*
         * توحيد ترتيب البيانات:
         * الأقدم ← الأحدث
         */

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

        /*
         * نبدأ من 6 أيام على الأقل
         * حتى يكون لدى المحرك بيانات كافية.
         *
         * كل اختبار:
         *
         * training = البيانات حتى اليوم الحالي
         * currentPrice = سعر اليوم الحالي
         * actualNextPrice = سعر اليوم التالي
         */

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

            /*
             * نحسب الاتجاه الصحيح فقط
             * عندما يكون المحرك قال صعود أو هبوط.
             *
             * الاتجاه العرضي لا يُحسب صحيحًا
             * إلا إذا كان السعر التالي شبه ثابت.
             */

            if (actualUp && predictedUp) {

                correctTests++;

            } else if (actualDown && predictedDown) {

                correctTests++;

            } else if (
                    Math.abs(
                            actualNextPrice
                                    - currentPrice
                    ) < 0.01
            ) {

                correctTests++;
            }

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
                    0
            );
        }

        double directionAccuracy =
                (correctTests * 100.0)
                        / totalTests;

        double averageAbsoluteError =
                totalAbsoluteError
                        / totalTests;

        return new BacktestResult(
                totalTests,
                correctTests,
                directionAccuracy,
                averageAbsoluteError
        );
    }

    public static class BacktestResult {

        public int totalTests;

        public int correctTests;

        public double directionAccuracy;

        public double averageAbsoluteError;

        public BacktestResult(
                int totalTests,
                int correctTests,
                double directionAccuracy,
                double averageAbsoluteError) {

            this.totalTests =
                    totalTests;

            this.correctTests =
                    correctTests;

            this.directionAccuracy =
                    directionAccuracy;

            this.averageAbsoluteError =
                    averageAbsoluteError;
        }
    }
}
