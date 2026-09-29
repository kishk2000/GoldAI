package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
                    0,
                    ""
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
        int correctSidewaysCount = 0;

        StringBuilder report =
                new StringBuilder();

        report.append(
                "🔎 تحليل تفصيلي للاختبار التاريخي\n\n"
        );

        /*
         * PredictionEngine يحتاج إلى 15 يومًا
         * على الأقل قبل إصدار أول توقع.
         *
         * لذلك يبدأ الاختبار من index 14.
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

            HistoricalGoldProvider.GoldBar currentBar =
                    chronological.get(i);

            HistoricalGoldProvider.GoldBar nextBar =
                    chronological.get(i + 1);

            double currentPrice =
                    currentBar.close;

            double actualNextPrice =
                    nextBar.close;

            PredictionEngine.PredictionResult result =
                    predictionEngine.analyze(
                            currentPrice,
                            training
                    );

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

            String actualDirection;

            if (actualUp) {
                actualDirection = "صعود ↑";
            } else if (actualDown) {
                actualDirection = "هبوط ↓";
            } else {
                actualDirection = "عرضي ↔";
            }

            String predictionResult;

            boolean correct = false;

            if (
                    predictedUp
                            && actualUp
            ) {

                correct = true;
                correctUpCount++;

            } else if (
                    predictedDown
                            && actualDown
            ) {

                correct = true;
                correctDownCount++;

            } else if (
                    predictedSideways
                            && actualSideways
            ) {

                correct = true;
                correctSidewaysCount++;
            }

            if (predictedUp) {
                predictedUpCount++;
            } else if (predictedDown) {
                predictedDownCount++;
            } else if (predictedSideways) {
                predictedSidewaysCount++;
            } else {
                continue;
            }

            if (correct) {
                predictionResult = "✅ صحيح";
                correctTests++;
            } else {
                predictionResult = "❌ خطأ";
            }

            double absoluteError =
                    Math.abs(
                            predictedPrice
                                    - actualNextPrice
                    );

            double actualChangePercent =
                    currentPrice != 0
                            ? (
                                    (
                                            actualNextPrice
                                                    - currentPrice
                                    )
                                            / currentPrice
                            ) * 100.0
                            : 0;

            double predictedChangePercent =
                    currentPrice != 0
                            ? (
                                    (
                                            predictedPrice
                                                    - currentPrice
                                    )
                                            / currentPrice
                            ) * 100.0
                            : 0;

            totalAbsoluteError +=
                    absoluteError;

            totalTests++;

            /*
             * إضافة الاختبار إلى التقرير.
             */

            report.append(
                    String.format(
                            Locale.US,

                            "اختبار %d\n"
                                    + "التاريخ: %s → %s\n"
                                    + "السعر الحالي: $%.2f\n"
                                    + "السعر المتوقع: $%.2f\n"
                                    + "السعر الفعلي: $%.2f\n"
                                    + "الاتجاه المتوقع: %s\n"
                                    + "الاتجاه الفعلي: %s\n"
                                    + "التغير الفعلي: %.2f%%\n"
                                    + "التغير المتوقع: %.2f%%\n"
                                    + "خطأ السعر: $%.2f\n"
                                    + "النتيجة: %s\n"
                                    + "-------------------------\n",

                            totalTests,

                            currentBar.date,
                            nextBar.date,

                            currentPrice,
                            predictedPrice,
                            actualNextPrice,

                            result.direction,
                            actualDirection,

                            actualChangePercent,
                            predictedChangePercent,

                            absoluteError,

                            predictionResult
                    )
            );
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
                    0,
                    report.toString()
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
                        ? (
                                correctUpCount
                                        * 100.0
                        )
                        / predictedUpCount
                        : 0;

        double downAccuracy =
                predictedDownCount > 0
                        ? (
                                correctDownCount
                                        * 100.0
                        )
                        / predictedDownCount
                        : 0;

        double sidewaysAccuracy =
                predictedSidewaysCount > 0
                        ? (
                                correctSidewaysCount
                                        * 100.0
                        )
                        / predictedSidewaysCount
                        : 0;

        /*
         * ملخص التحليل في بداية التقرير.
         */

        String summary =
                String.format(
                        Locale.US,

                        "📊 ملخص الاختبار التاريخي\n\n"
                                + "إجمالي الاختبارات: %d\n"
                                + "التوقعات الصحيحة: %d\n"
                                + "التوقعات الخاطئة: %d\n"
                                + "دقة الاتجاه: %.1f%%\n"
                                + "متوسط خطأ السعر: $%.2f\n\n"

                                + "⬆️ الصعود:\n"
                                + "التوقعات: %d\n"
                                + "الصحيحة: %d\n"
                                + "الدقة: %.1f%%\n\n"

                                + "⬇️ الهبوط:\n"
                                + "التوقعات: %d\n"
                                + "الصحيحة: %d\n"
                                + "الدقة: %.1f%%\n\n"

                                + "↔️ العرضي:\n"
                                + "التوقعات: %d\n"
                                + "الصحيحة: %d\n"
                                + "الدقة: %.1f%%\n\n"

                                + "=========================\n\n",

                        totalTests,
                        correctTests,
                        totalTests - correctTests,

                        directionAccuracy,
                        averageAbsoluteError,

                        predictedUpCount,
                        correctUpCount,
                        upAccuracy,

                        predictedDownCount,
                        correctDownCount,
                        downAccuracy,

                        predictedSidewaysCount,
                        correctSidewaysCount,
                        sidewaysAccuracy
                );

        report.insert(
                0,
                summary
        );

        return new BacktestResult(
                totalTests,
                correctTests,
                directionAccuracy,
                averageAbsoluteError,
                predictedUpCount,
                predictedDownCount,
                predictedSidewaysCount,
                upAccuracy,
                downAccuracy,
                report.toString()
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

        public String detailedReport;

        public BacktestResult(
                int totalTests,
                int correctTests,
                double directionAccuracy,
                double averageAbsoluteError,
                int predictedUp,
                int predictedDown,
                int predictedSideways,
                double upAccuracy,
                double downAccuracy,
                String detailedReport) {

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

            this.detailedReport =
                    detailedReport;
        }
    }
}
