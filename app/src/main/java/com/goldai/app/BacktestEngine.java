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
                (a, b) -> a.date.compareTo(b.date)
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

        int weakTests = 0;
        int mediumTests = 0;
        int strongTests = 0;

        int weakCorrect = 0;
        int mediumCorrect = 0;
        int strongCorrect = 0;

        double totalForecastChange = 0;
        double totalActualChange = 0;

        double upForecastChange = 0;
        double upActualChange = 0;
        double upAbsoluteError = 0;
        int upTests = 0;

        double downForecastChange = 0;
        double downActualChange = 0;
        double downAbsoluteError = 0;
        int downTests = 0;

        double sidewaysForecastChange = 0;
        double sidewaysActualChange = 0;
        double sidewaysAbsoluteError = 0;
        int sidewaysTests = 0;

        int[][] confusionMatrix =
                new int[3][3];

        StringBuilder report =
                new StringBuilder();

        report.append(
                "🔎 تحليل تفصيلي للاختبار التاريخي\n\n"
        );

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

            /*
             * تجاهل حالة نقص البيانات فقط.
             */
            if (result.direction.equals(
                    "بيانات غير كافية"
            )) {
                continue;
            }

            /*
             * ==============================
             * التغير الفعلي
             * ==============================
             */

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

            boolean actualUp =
                    actualChangePercent >= 0.30;

            boolean actualDown =
                    actualChangePercent <= -0.30;

            boolean actualSideways =
                    Math.abs(
                            actualChangePercent
                    ) < 0.30;

            /*
             * ==============================
             * اتجاه التوقع
             *
             * PredictionEngine الحالي يستخدم:
             * صاعد / هابط / محايد
             * ==============================
             */

            boolean predictedUp =
                    result.direction.contains(
                            "صاعد"
                    );

            boolean predictedDown =
                    result.direction.contains(
                            "هابط"
                    );

            boolean predictedSideways =
                    result.direction.contains(
                            "محايد"
                    );

            /*
             * توافق إضافي مع أي صيغة قديمة.
             */
            if (!predictedUp &&
                    !predictedDown &&
                    !predictedSideways) {

                predictedUp =
                        result.direction.contains(
                                "صعود"
                        );

                predictedDown =
                        result.direction.contains(
                                "هبوط"
                        );

                predictedSideways =
                        result.direction.contains(
                                "عرضي"
                        );
            }

            /*
             * إذا لم نتعرف على الاتجاه،
             * لا ندخله في الاختبار.
             */
            if (!predictedUp &&
                    !predictedDown &&
                    !predictedSideways) {

                continue;
            }

            String actualDirection;

            int actualIndex;

            if (actualUp) {

                actualDirection = "صعود ↑";
                actualIndex = 0;

            } else if (actualDown) {

                actualDirection = "هبوط ↓";
                actualIndex = 1;

            } else {

                actualDirection = "عرضي ↔";
                actualIndex = 2;
            }

            int predictedIndex;

            if (predictedUp) {

                predictedIndex = 0;

            } else if (predictedDown) {

                predictedIndex = 1;

            } else {

                predictedIndex = 2;
            }

            /*
             * ==============================
             * مصفوفة التوقع والواقع
             * ==============================
             */

            confusionMatrix[predictedIndex][actualIndex]++;

            /*
             * ==============================
             * تحديد صحة التوقع
             * ==============================
             */

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

            /*
             * ==============================
             * عدد التوقعات
             * ==============================
             */

            if (predictedUp) {

                predictedUpCount++;

            } else if (predictedDown) {

                predictedDownCount++;

            } else {

                predictedSidewaysCount++;
            }

            /*
             * ==============================
             * التغير المتوقع
             * ==============================
             */

            double forecastChangePercent =
                    currentPrice != 0
                            ? (
                                    (
                                            result.predictedPrice
                                                    - currentPrice
                                    )
                                            / currentPrice
                            ) * 100.0
                            : 0;

            /*
             * ==============================
             * قوة الإشارة
             * ==============================
             */

            double signalStrength =
                    Math.abs(
                            forecastChangePercent
                    );

            String signalLevel;

            if (signalStrength < 0.30) {

                signalLevel = "ضعيفة";

                weakTests++;

                if (correct) {
                    weakCorrect++;
                }

            } else if (signalStrength < 1.00) {

                signalLevel = "متوسطة";

                mediumTests++;

                if (correct) {
                    mediumCorrect++;
                }

            } else {

                signalLevel = "قوية";

                strongTests++;

                if (correct) {
                    strongCorrect++;
                }
            }

            /*
             * ==============================
             * خطأ السعر
             * ==============================
             */

            double absoluteError =
                    Math.abs(
                            result.predictedPrice
                                    - actualNextPrice
                    );

            /*
             * ==============================
             * الإحصائيات العامة
             * ==============================
             */

            totalForecastChange +=
                    forecastChangePercent;

            totalActualChange +=
                    actualChangePercent;

            totalAbsoluteError +=
                    absoluteError;

            /*
             * ==============================
             * إحصائيات حسب الاتجاه
             * ==============================
             */

            if (predictedUp) {

                upTests++;

                upForecastChange +=
                        forecastChangePercent;

                upActualChange +=
                        actualChangePercent;

                upAbsoluteError +=
                        absoluteError;

            } else if (predictedDown) {

                downTests++;

                downForecastChange +=
                        forecastChangePercent;

                downActualChange +=
                        actualChangePercent;

                downAbsoluteError +=
                        absoluteError;

            } else {

                sidewaysTests++;

                sidewaysForecastChange +=
                        forecastChangePercent;

                sidewaysActualChange +=
                        actualChangePercent;

                sidewaysAbsoluteError +=
                        absoluteError;
            }

            /*
             * ==============================
             * النتيجة
             * ==============================
             */

            String predictionResult;

            if (correct) {

                predictionResult = "✅ صحيح";
                correctTests++;

            } else {

                predictionResult = "❌ خطأ";
            }

            totalTests++;

            /*
             * ==============================
             * المؤشرات
             * ==============================
             */

            double ema5 =
                    calculateEMA(
                            training,
                            5
                    );

            double ema10 =
                    calculateEMA(
                            training,
                            10
                    );

            double ema20 =
                    calculateEMA(
                            training,
                            20
                    );

            double rsi =
                    calculateRSI(
                            training,
                            14
                    );

            double momentum1 =
                    calculateMomentum(
                            training,
                            1
                    ) * 100.0;

            double momentum3 =
                    calculateMomentum(
                            training,
                            3
                    ) * 100.0;

            double momentum5 =
                    calculateMomentum(
                            training,
                            5
                    ) * 100.0;

            /*
             * ==============================
             * التقرير التفصيلي
             * ==============================
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
                                    + "قوة الإشارة: %.2f%% (%s)\n"
                                    + "RSI: %.1f\n"
                                    + "EMA5: $%.2f\n"
                                    + "EMA10: $%.2f\n"
                                    + "EMA20: $%.2f\n"
                                    + "Momentum 1D: %.2f%%\n"
                                    + "Momentum 3D: %.2f%%\n"
                                    + "Momentum 5D: %.2f%%\n"
                                    + "خطأ السعر: $%.2f\n"
                                    + "الثقة: %.0f%%\n"
                                    + "النتيجة: %s\n"
                                    + "-------------------------\n",

                            totalTests,

                            currentBar.date,
                            nextBar.date,

                            currentPrice,
                            result.predictedPrice,
                            actualNextPrice,

                            result.direction,
                            actualDirection,

                            actualChangePercent,
                            forecastChangePercent,

                            signalStrength,
                            signalLevel,

                            rsi,

                            ema5,
                            ema10,
                            ema20,

                            momentum1,
                            momentum3,
                            momentum5,

                            absoluteError,

                            result.confidence,

                            predictionResult
                    )
            );
        }

        /*
         * ==============================
         * لا توجد اختبارات
         * ==============================
         */

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

        /*
         * ==============================
         * الحسابات الأساسية
         * ==============================
         */

        double directionAccuracy =
                (
                        correctTests * 100.0
                )
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

        double weakAccuracy =
                weakTests > 0
                        ? (
                                weakCorrect * 100.0
                        )
                        / weakTests
                        : 0;

        double mediumAccuracy =
                mediumTests > 0
                        ? (
                                mediumCorrect * 100.0
                        )
                        / mediumTests
                        : 0;

        double strongAccuracy =
                strongTests > 0
                        ? (
                                strongCorrect * 100.0
                        )
                        / strongTests
                        : 0;

        double averageForecastChange =
                totalForecastChange
                        / totalTests;

        double averageActualChange =
                totalActualChange
                        / totalTests;

        double averageUpForecast =
                upTests > 0
                        ? upForecastChange / upTests
                        : 0;

        double averageUpActual =
                upTests > 0
                        ? upActualChange / upTests
                        : 0;

        double averageUpError =
                upTests > 0
                        ? upAbsoluteError / upTests
                        : 0;

        double averageDownForecast =
                downTests > 0
                        ? downForecastChange / downTests
                        : 0;

        double averageDownActual =
                downTests > 0
                        ? downActualChange / downTests
                        : 0;

        double averageDownError =
                downTests > 0
                        ? downAbsoluteError / downTests
                        : 0;

        double averageSidewaysForecast =
                sidewaysTests > 0
                        ? sidewaysForecastChange
                                / sidewaysTests
                        : 0;

        double averageSidewaysActual =
                sidewaysTests > 0
                        ? sidewaysActualChange
                                / sidewaysTests
                        : 0;

        double averageSidewaysError =
                sidewaysTests > 0
                        ? sidewaysAbsoluteError
                                / sidewaysTests
                        : 0;

        double forecastBias =
                averageForecastChange
                        - averageActualChange;

        /*
         * ==============================
         * مصفوفة الالتباس
         * ==============================
         */

        String confusionReport =
                String.format(
                        Locale.US,

                        "🧩 مقارنة التوقع بالواقع\n\n"

                                + "                 الواقع\n"
                                + "              ↑      ↓      ↔\n"

                                + "توقع ↑      %d      %d      %d\n"
                                + "توقع ↓      %d      %d      %d\n"
                                + "توقع ↔      %d      %d      %d\n\n",

                        confusionMatrix[0][0],
                        confusionMatrix[0][1],
                        confusionMatrix[0][2],

                        confusionMatrix[1][0],
                        confusionMatrix[1][1],
                        confusionMatrix[1][2],

                        confusionMatrix[2][0],
                        confusionMatrix[2][1],
                        confusionMatrix[2][2]
                );

        /*
         * ==============================
         * تقرير التشخيص
         * ==============================
         */

        String diagnosticReport =
                String.format(
                        Locale.US,

                        "🧠 تشخيص انحياز المحرك\n\n"

                                + "متوسط التغير المتوقع: %.2f%%\n"
                                + "متوسط التغير الفعلي: %.2f%%\n"
                                + "انحياز التوقع: %.2f نقطة مئوية\n\n"

                                + "⬆️ عند توقع الصعود:\n"
                                + "عدد الاختبارات: %d\n"
                                + "متوسط التغير المتوقع: %.2f%%\n"
                                + "متوسط التغير الفعلي: %.2f%%\n"
                                + "متوسط خطأ السعر: $%.2f\n"
                                + "دقة توقع الصعود: %.1f%%\n\n"

                                + "⬇️ عند توقع الهبوط:\n"
                                + "عدد الاختبارات: %d\n"
                                + "متوسط التغير المتوقع: %.2f%%\n"
                                + "متوسط التغير الفعلي: %.2f%%\n"
                                + "متوسط خطأ السعر: $%.2f\n"
                                + "دقة توقع الهبوط: %.1f%%\n\n"

                                + "↔️ عند توقع العرضي:\n"
                                + "عدد الاختبارات: %d\n"
                                + "متوسط التغير المتوقع: %.2f%%\n"
                                + "متوسط التغير الفعلي: %.2f%%\n"
                                + "متوسط خطأ السعر: $%.2f\n"
                                + "دقة التوقع العرضي: %.1f%%\n\n",

                        averageForecastChange,
                        averageActualChange,
                        forecastBias,

                        upTests,
                        averageUpForecast,
                        averageUpActual,
                        averageUpError,
                        upAccuracy,

                        downTests,
                        averageDownForecast,
                        averageDownActual,
                        averageDownError,
                        downAccuracy,

                        sidewaysTests,
                        averageSidewaysForecast,
                        averageSidewaysActual,
                        averageSidewaysError,
                        sidewaysAccuracy
                );

        /*
         * ==============================
         * ملخص الاختبار
         * ==============================
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

                                + "📈 الدقة حسب قوة الإشارة:\n\n"

                                + "ضعيفة (<0.30%%):\n"
                                + "الاختبارات: %d\n"
                                + "الصحيحة: %d\n"
                                + "الدقة: %.1f%%\n\n"

                                + "متوسطة (0.30%%–1.00%%):\n"
                                + "الاختبارات: %d\n"
                                + "الصحيحة: %d\n"
                                + "الدقة: %.1f%%\n\n"

                                + "قوية (≥1.00%%):\n"
                                + "الاختبارات: %d\n"
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
                        sidewaysAccuracy,

                        weakTests,
                        weakCorrect,
                        weakAccuracy,

                        mediumTests,
                        mediumCorrect,
                        mediumAccuracy,

                        strongTests,
                        strongCorrect,
                        strongAccuracy
                );

        report.insert(
                0,
                summary
                        + diagnosticReport
                        + confusionReport
                        + "\n"
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

    private double calculateEMA(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        if (
                bars == null ||
                        bars.isEmpty()
        ) {
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
                2.0 /
                        (count + 1.0);

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
