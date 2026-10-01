package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BacktestEngine {

    private final PredictionEngine predictionEngine = new PredictionEngine();

    public BacktestResult run(List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null || bars.size() < 16) {
            return new BacktestResult(0, 0, 0, 0, 0, 0, 0, 0, 0, "بيانات غير كافية لإجراء الاختبار التاريخي");
        }

        List<HistoricalGoldProvider.GoldBar> chronological = new ArrayList<>(bars);

        // الترتيب باستخدام date لتوافق الكلاس الموجود في HistoricalGoldProvider
        Collections.sort(chronological, (a, b) -> {
            if (a.date == null || b.date == null) return 0;
            return a.date.compareTo(b.date);
        });

        int totalTests = 0;
        int correctTests = 0;
        double totalAbsoluteError = 0;

        int predictedUpCount = 0;
        int predictedDownCount = 0;
        int predictedSidewaysCount = 0;

        int correctUpCount = 0;
        int correctDownCount = 0;
        int correctSidewaysCount = 0;

        int weakTests = 0, mediumTests = 0, strongTests = 0;
        int weakCorrect = 0, mediumCorrect = 0, strongCorrect = 0;

        double totalForecastChange = 0, totalActualChange = 0;
        double upForecastChange = 0, upActualChange = 0, upAbsoluteError = 0;
        int upTests = 0;

        double downForecastChange = 0, downActualChange = 0, downAbsoluteError = 0;
        int downTests = 0;

        double sidewaysForecastChange = 0, sidewaysActualChange = 0, sidewaysAbsoluteError = 0;
        int sidewaysTests = 0;

        int[][] confusionMatrix = new int[3][3];
        StringBuilder report = new StringBuilder();
        report.append("🔎 تحليل تفصيلي للاختبار التاريخي\n\n");

        for (int i = 14; i < chronological.size() - 1; i++) {

            List<HistoricalGoldProvider.GoldBar> training = new ArrayList<>(chronological.subList(0, i + 1));
            HistoricalGoldProvider.GoldBar currentBar = chronological.get(i);
            HistoricalGoldProvider.GoldBar nextBar = chronological.get(i + 1);

            if (currentBar == null || nextBar == null) continue;

            double currentPrice = currentBar.close;
            double actualNextPrice = nextBar.close;

            PredictionEngine.PredictionResult result = null;
            try {
                result = predictionEngine.analyze(currentPrice, training);
            } catch (Exception e) {
                continue;
            }

            if (result == null || result.direction == null || result.direction.contains("غير كافية")) {
                continue;
            }

            double actualChangePercent = currentPrice != 0 ? ((actualNextPrice - currentPrice) / currentPrice) * 100.0 : 0;

            boolean actualUp = actualChangePercent >= 0.30;
            boolean actualDown = actualChangePercent <= -0.30;
            boolean actualSideways = Math.abs(actualChangePercent) < 0.30;

            boolean predictedUp = result.direction.contains("صاعد") || result.direction.contains("صعود");
            boolean predictedDown = result.direction.contains("هابط") || result.direction.contains("هبوط");
            boolean predictedSideways = result.direction.contains("محايد") || result.direction.contains("عرضي");

            if (!predictedUp && !predictedDown && !predictedSideways) {
                continue;
            }

            String actualDirection = actualUp ? "صعود ↑" : (actualDown ? "هبوط ↓" : "عرضي ↔");
            int actualIndex = actualUp ? 0 : (actualDown ? 1 : 2);
            int predictedIndex = predictedUp ? 0 : (predictedDown ? 1 : 2);

            confusionMatrix[predictedIndex][actualIndex]++;

            boolean correct = (predictedUp && actualUp) || (predictedDown && actualDown) || (predictedSideways && actualSideways);

            if (correct) {
                if (predictedUp) correctUpCount++;
                else if (predictedDown) correctDownCount++;
                else correctSidewaysCount++;
            }

            if (predictedUp) predictedUpCount++;
            else if (predictedDown) predictedDownCount++;
            else predictedSidewaysCount++;

            double forecastChangePercent = currentPrice != 0 ? ((result.predictedPrice - currentPrice) / currentPrice) * 100.0 : 0;
            double signalStrength = Math.abs(forecastChangePercent);

            String signalLevel;
            if (signalStrength < 0.30) {
                signalLevel = "ضعيفة";
                weakTests++;
                if (correct) weakCorrect++;
            } else if (signalStrength < 1.00) {
                signalLevel = "متوسطة";
                mediumTests++;
                if (correct) mediumCorrect++;
            } else {
                signalLevel = "قوية";
                strongTests++;
                if (correct) strongCorrect++;
            }

            double absoluteError = Math.abs(result.predictedPrice - actualNextPrice);

            totalForecastChange += forecastChangePercent;
            totalActualChange += actualChangePercent;
            totalAbsoluteError += absoluteError;

            if (predictedUp) {
                upTests++;
                upForecastChange += forecastChangePercent;
                upActualChange += actualChangePercent;
                upAbsoluteError += absoluteError;
            } else if (predictedDown) {
                downTests++;
                downForecastChange += forecastChangePercent;
                downActualChange += actualChangePercent;
                downAbsoluteError += absoluteError;
            } else {
                sidewaysTests++;
                sidewaysForecastChange += forecastChangePercent;
                sidewaysActualChange += actualChangePercent;
                sidewaysAbsoluteError += absoluteError;
            }

            String predictionResultStr = correct ? "✅ صحيح" : "❌ خطأ";
            if (correct) correctTests++;
            totalTests++;

            double ema5 = calculateEMA(training, 5);
            double ema10 = calculateEMA(training, 10);
            double ema20 = calculateEMA(training, 20);
            double rsi = calculateRSI(training, 14);
            double momentum1 = calculateMomentum(training, 1) * 100.0;
            double momentum3 = calculateMomentum(training, 3) * 100.0;
            double momentum5 = calculateMomentum(training, 5) * 100.0;

            report.append(String.format(Locale.US,
                    "اختبار %d\nالتاريخ: %s → %s\nالسعر الحالي: $%.2f\nالسعر المتوقع: $%.2f\nالسعر الفعلي: $%.2f\n" +
                    "الاتجاه المتوقع: %s\nالاتجاه الفعلي: %s\nالتغير الفعلي: %.2f%%\nالتغير المتوقع: %.2f%%\n" +
                    "قوة الإشارة: %.2f%% (%s)\nRSI: %.1f\nEMA5: $%.2f\nEMA10: $%.2f\nEMA20: $%.2f\n" +
                    "Momentum 1D: %.2f%%\nMomentum 3D: %.2f%%\nMomentum 5D: %.2f%%\nخطأ السعر: $%.2f\nالثقة: %.0f%%\n" +
                    "النتيجة: %s\n-------------------------\n",
                    totalTests, currentBar.date, nextBar.date, currentPrice, result.predictedPrice, actualNextPrice,
                    result.direction, actualDirection, actualChangePercent, forecastChangePercent, signalStrength,
                    signalLevel, rsi, ema5, ema10, ema20, momentum1, momentum3, momentum5, absoluteError,
                    result.confidence, predictionResultStr));
        }

        if (totalTests == 0) {
            return new BacktestResult(0, 0, 0, 0, 0, 0, 0, 0, 0, report.toString());
        }

        double directionAccuracy = (correctTests * 100.0) / totalTests;
        double averageAbsoluteError = totalAbsoluteError / totalTests;

        double upAccuracy = predictedUpCount > 0 ? (correctUpCount * 100.0) / predictedUpCount : 0;
        double downAccuracy = predictedDownCount > 0 ? (correctDownCount * 100.0) / predictedDownCount : 0;
        double sidewaysAccuracy = predictedSidewaysCount > 0 ? (correctSidewaysCount * 100.0) / predictedSidewaysCount : 0;

        double weakAccuracy = weakTests > 0 ? (weakCorrect * 100.0) / weakTests : 0;
        double mediumAccuracy = mediumTests > 0 ? (mediumCorrect * 100.0) / mediumTests : 0;
        double strongAccuracy = strongTests > 0 ? (strongCorrect * 100.0) / strongTests : 0;

        double averageForecastChange = totalForecastChange / totalTests;
        double averageActualChange = totalActualChange / totalTests;

        double averageUpForecast = upTests > 0 ? upForecastChange / upTests : 0;
        double averageUpActual = upTests > 0 ? upActualChange / upTests : 0;
        double averageUpError = upTests > 0 ? upAbsoluteError / upTests : 0;

        double averageDownForecast = downTests > 0 ? downForecastChange / downTests : 0;
        double averageDownActual = downTests > 0 ? downActualChange / downTests : 0;
        double averageDownError = downTests > 0 ? downAbsoluteError / downTests : 0;

        double averageSidewaysForecast = sidewaysTests > 0 ? sidewaysForecastChange / sidewaysTests : 0;
        double averageSidewaysActual = sidewaysTests > 0 ? sidewaysActualChange / sidewaysTests : 0;
        double averageSidewaysError = sidewaysTests > 0 ? sidewaysAbsoluteError / sidewaysTests : 0;

        double forecastBias = averageForecastChange - averageActualChange;

        String confusionReport = String.format(Locale.US,
                "🧩 مقارنة التوقع بالواقع\n\n                 الواقع\n              ↑      ↓      ↔\n" +
                "توقع ↑      %d      %d      %d\nتوقع ↓      %d      %d      %d\nتوقع ↔      %d      %d      %d\n\n",
                confusionMatrix[0][0], confusionMatrix[0][1], confusionMatrix[0][2],
                confusionMatrix[1][0], confusionMatrix[1][1], confusionMatrix[1][2],
                confusionMatrix[2][0], confusionMatrix[2][1], confusionMatrix[2][2]);

        String diagnosticReport = String.format(Locale.US,
                "🧠 تشخيص انحياز المحرك\n\nمتوسط التغير المتوقع: %.2f%%\nمتوسط التغير الفعلي: %.2f%%\nانحياز التوقع: %.2f نقطة مئوية\n\n" +
                "⬆️ عند توقع الصعود:\nعدد الاختبارات: %d\nمتوسط التغير المتوقع: %.2f%%\nمتوسط التغير الفعلي: %.2f%%\nمتوسط خطأ السعر: $%.2f\nدقة توقع الصعود: %.1f%%\n\n" +
                "⬇️️ عند توقع الهبوط:\nعدد الاختبارات: %d\nمتوسط التغير المتوقع: %.2f%%\nمتوسط التغير الفعلي: %.2f%%\nمتوسط خطأ السعر: $%.2f\nدقة توقع الهبوط: %.1f%%\n\n" +
                "↔️ عند توقع العرضي:\nعدد الاختبارات: %d\nمتوسط التغير المتوقع: %.2f%%\nمتوسط التغير الفعلي: %.2f%%\nمتوسط خطأ السعر: $%.2f\nدقة التوقع العرضي: %.1f%%\n\n",
                averageForecastChange, averageActualChange, forecastBias,
                upTests, averageUpForecast, averageUpActual, averageUpError, upAccuracy,
                downTests, averageDownForecast, averageDownActual, averageDownError, downAccuracy,
                sidewaysTests, averageSidewaysForecast, averageSidewaysActual, averageSidewaysError, sidewaysAccuracy);

        String summary = String.format(Locale.US,
                "📊 ملخص الاختبار التاريخي\n\nإجمالي الاختبارات: %d\nالتوقعات الصحيحة: %d\nالتوقعات الخاطئة: %d\nدقة الاتجاه: %.1f%%\nمتوسط خطأ السعر: $%.2f\n\n" +
                "⬆️ الصعود:\nالتوقعات: %d\nالصحيحة: %d\nالدقة: %.1f%%\n\n⬇️️ الهبوط:\nالتوقعات: %d\nالصحيحة: %d\nالدقة: %.1f%%\n\n↔ العرضي:\nالتوقعات: %d\nالصحيحة: %d\nالدقة: %.1f%%\n\n" +
                "📈 الدقة حسب قوة الإشارة:\n\nضعيفة (<0.30%%):\nالاختبارات: %d\nالصحيحة: %d\nالدقة: %.1f%%\n\nمتوسطة (0.30%%–1.00%%):\nالاختبارات: %d\nالصحيحة: %d\nالدقة: %.1f%%\n\nقوية (≥1.00%%):\nالاختبارات: %d\nالصحيحة: %d\nالدقة: %.1f%%\n\n=========================\n\n",
                totalTests, correctTests, totalTests - correctTests, directionAccuracy, averageAbsoluteError,
                predictedUpCount, correctUpCount, upAccuracy, predictedDownCount, correctDownCount, downAccuracy,
                predictedSidewaysCount, correctSidewaysCount, sidewaysAccuracy,
                weakTests, weakCorrect, weakAccuracy, mediumTests, mediumCorrect, mediumAccuracy, strongTests, strongCorrect, strongAccuracy);

        report.insert(0, summary + diagnosticReport + confusionReport + "\n");

        return new BacktestResult(totalTests, correctTests, directionAccuracy, averageAbsoluteError,
                predictedUpCount, predictedDownCount, predictedSidewaysCount, upAccuracy, downAccuracy, report.toString());
    }

    private double calculateEMA(List<HistoricalGoldProvider.GoldBar> bars, int period) {
        if (bars == null || bars.isEmpty()) return 0;
        int count = Math.min(period, bars.size());
        int start = bars.size() - count;
        double ema = bars.get(start).close;
        double multiplier = 2.0 / (count + 1.0);
        for (int i = start + 1; i < bars.size(); i++) {
            ema = (bars.get(i).close - ema) * multiplier + ema;
        }
        return ema;
    }

    private double calculateRSI(List<HistoricalGoldProvider.GoldBar> bars, int period) {
        if (bars == null || bars.size() < 2) return 50;
        period = Math.min(period, bars.size() - 1);
        double gains = 0, losses = 0;
        int start = bars.size() - period;
        for (int i = start; i < bars.size(); i++) {
            double change = bars.get(i).close - bars.get(i - 1).close;
            if (change > 0) gains += change;
            else if (change < 0) losses -= change;
        }
        double averageGain = gains / period;
        double averageLoss = losses / period;
        if (averageLoss == 0) return averageGain == 0 ? 50 : 100;
        return 100 - (100 / (1 + (averageGain / averageLoss)));
    }

    private double calculateMomentum(List<HistoricalGoldProvider.GoldBar> bars, int period) {
        if (bars == null || bars.size() <= period) return 0;
        int latestIndex = bars.size() - 1;
        double latest = bars.get(latestIndex).close;
        double previous = bars.get(latestIndex - period).close;
        if (previous == 0) return 0;
        return (latest - previous) / previous;
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

        public BacktestResult(int totalTests, int correctTests, double directionAccuracy,
                              double averageAbsoluteError, int predictedUp, int predictedDown,
                              int predictedSideways, double upAccuracy, double downAccuracy, String detailedReport) {
            this.totalTests = totalTests;
            this.correctTests = correctTests;
            this.directionAccuracy = directionAccuracy;
            this.averageAbsoluteError = averageAbsoluteError;
            this.predictedUp = predictedUp;
            this.predictedDown = predictedDown;
            this.predictedSideways = predictedSideways;
            this.upAccuracy = upAccuracy;
            this.downAccuracy = downAccuracy;
            this.detailedReport = detailedReport;
        }
    }
}
