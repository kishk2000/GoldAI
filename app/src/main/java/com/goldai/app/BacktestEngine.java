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

        /*
         * تحليل حسب قوة الإشارة
         */
        int weakTests = 0;
        int mediumTests = 0;
        int strongTests = 0;

        int weakCorrect = 0;
        int mediumCorrect = 0;
        int strongCorrect = 0;

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

            if (result.direction.equals(
                    "بيانات غير كافية"
            )) {
                continue;
            }

            /*
             * ==========================
             * الحركة الفعلية
             * ==========================
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
             * ==========================
             * الحركة المتوقعة
             * ==========================
             */

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

                actualDirection =
                        "صعود ↑";

            } else if (actualDown) {

                actualDirection =
                        "هبوط ↓";

            } else {

                actualDirection =
                        "عرضي ↔";
            }

            /*
             * ==========================
             * صحة التوقع
             * ==========================
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

            if (predictedUp) {

                predictedUpCount++;

            } else if (predictedDown) {

                predictedDownCount++;

            } else if (predictedSideways) {

                predictedSidewaysCount++;

            } else {

                continue;
            }

            /*
             * ==========================
             * قوة التوقع
             * ==========================
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

            double signalStrength =
                    Math.abs(
                            forecastChangePercent
                    );

            String signalLevel;

            if (signalStrength < 0.30) {

                signalLevel =
                        "ضعيفة";

                weakTests++;

                if (correct) {
                    weakCorrect++;
                }

            } else if (signalStrength < 1.00) {

                signalLevel =
                        "متوسطة";

                mediumTests++;

                if (correct) {
                    mediumCorrect++;
                }

            } else {

                signalLevel =
                        "قوية";

                strongTests++;

                if (correct) {
                    strongCorrect++;
                }
            }

            /*
             * ==========================
             * النتيجة
             * ==========================
             */

            String predictionResult;

            if (correct) {

                predictionResult =
                        "✅ صحيح";

                correctTests++;

            } else {

                predictionResult =
                        "❌ خطأ";
            }

            double absoluteError =
                    Math.abs(
                            result.predictedPrice
                                    - actualNextPrice
                    );

            totalAbsoluteError +=
                    absoluteError;

            totalTests++;

            /*
             * ==========================
             * المؤشرات المستخدمة
             * ==========================
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
             * ==========================
             * التقرير
             * ==========================
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
         * ==========================
         * الإحصائيات الأساسية
         * ==========================
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

        /*
         * ==========================
         * دقة قوة الإشارة
         * ==========================
         */

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

        /*
         * ==========================
         * الملخص
         * ==========================
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

    /*
     * ==========================
     * EMA
     * ==========================
     */

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

    /*
     * ==========================
     * RSI
     * ==========================
     */

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

    /*
     * ==========================
     * Momentum
     * ==========================
     */

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

    /*
     * ==========================
     * Backtest Result
     * ==========================
     */

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
