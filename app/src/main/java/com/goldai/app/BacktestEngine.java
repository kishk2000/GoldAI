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

        if (bars == null || bars.size() < 16) {

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

        int predictedUpCount = 0;
        int predictedDownCount = 0;
        int predictedSidewaysCount = 0;

        int correctUpCount = 0;
        int correctDownCount = 0;

        /*
         * PredictionEngine يحتاج إلى 15 يومًا على الأقل.
         *
         * لذلك نبدأ من index 14.
         *
         * مثال:
         * الأيام 0 → 14 = 15 يوم تدريب
         * اليوم 15 = النتيجة الفعلية التي سنختبر عليها
         */

        for (
                int i = 14;
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

            /*
             * تجاهل أي نتيجة غير صالحة
             * في حالة عدم كفاية البيانات.
             */

            if (result.direction.equals(
                    "بيانات غير كافية"
            )) {

                continue;
            }

            double predictedPrice =
                    result.predictedPrice;

            boolean actualUp =
                    actualNextPrice > currentPrice;

            boolean actualDown =
                    actualNextPrice < currentPrice;

            boolean actualSideways =
                    Math.abs(
                            actualNextPrice
                                    - currentPrice
                    ) < 0.01;

            boolean isPredictedUp =
                    result.direction.contains(
                            "صعود"
                    );

            boolean isPredictedDown =
                    result.direction.contains(
                            "هبوط"
                    );

            boolean isPredictedSideways =
                    result.direction.contains(
                            "عرضي"
                    );

            /*
             * عدد التوقعات
             */

            if (isPredictedUp) {

                predictedUpCount++;

            } else if (isPredictedDown) {

                predictedDownCount++;

            } else if (isPredictedSideways) {

                predictedSidewaysCount++;

            } else {

                // نتيجة غير معروفة، لا تدخل في الاختبار
                continue;
            }

            /*
             * حساب الدقة
             */

            if (actualUp && isPredictedUp) {

                correctTests++;
                correctUpCount++;

            } else if (actualDown && isPredictedDown) {

                correctTests++;
                correctDownCount++;

            } else if (
                    actualSideways
                            && isPredictedSideways
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
                predictedUpCount > 0
                        ? (correctUpCount * 100.0)
                        / predictedUpCount
                        : 0;

        double downAccuracy =
                predictedDownCount > 0
                        ? (correctDownCount * 100.0)
                        / predictedDownCount
                        : 0;

        return new BacktestResult(
                totalTests,
                correctTests,
                directionAccuracy,
                averageAbsoluteError,
                predictedUpCount,
                predictedDownCount,
                predictedSidewaysCount,
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
