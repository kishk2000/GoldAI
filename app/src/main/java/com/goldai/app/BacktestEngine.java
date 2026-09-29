package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
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
         * البيانات من GoldPrice.dev تأتي من الأحدث إلى الأقدم.
         * نقلبها حتى يصبح التاريخ:
         *
         * قديم → جديد
         */
        List<HistoricalGoldProvider.GoldBar> chronological =
                new ArrayList<>(bars);

        java.util.Collections.reverse(
                chronological
        );

        int totalTests = 0;
        int correctTests = 0;

        double totalAbsoluteError = 0;

        /*
         * نحتاج على الأقل 5 أيام قبل أن نبدأ التوقع.
         *
         * في كل اختبار:
         *
         * 1- نستخدم الأيام السابقة فقط.
         * 2- نتوقع السعر التالي.
         * 3- نقارن التوقع بالسعر الحقيقي.
         */
        for (
                int i = 5;
                i < chronological.size() - 1;
                i++
        ) {

            List<HistoricalGoldProvider.GoldBar> training =
                    new ArrayList<>(
                            chronological.subList(
                                    0,
                                    i
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

            /*
             * اتجاه السعر الحقيقي
             */
            boolean actualUp =
                    actualNextPrice > currentPrice;

            boolean actualDown =
                    actualNextPrice < currentPrice;

            /*
             * الاتجاه الذي توقعه المحرك
             */
            boolean predictedUp =
                    result.direction.contains(
                            "صعود"
                    );

            boolean predictedDown =
                    result.direction.contains(
                            "هبوط"
                    );

            /*
             * نحسب صحة الاتجاه.
             * لو الحركة الفعلية كانت شبه ثابتة
             * لا نعتبرها خطأ توقع.
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
