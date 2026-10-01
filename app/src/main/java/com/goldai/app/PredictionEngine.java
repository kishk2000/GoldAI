package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.List;

public class PredictionEngine {

    private static final double THRESHOLD = 0.002; // 0.2% عتبة الاتجاه
    private static final int MIN_HISTORY = 20;

    public static class PredictionResult {
        public double predictedPrice;
        public String direction;
        public int confidence;

        public PredictionResult(double predictedPrice, String direction, int confidence) {
            this.predictedPrice = predictedPrice;
            this.direction = direction;
            this.confidence = confidence;
        }
    }

    public PredictionResult analyze(double currentPrice, List<HistoricalGoldProvider.GoldBar> bars) {
        if (bars == null || bars.size() < MIN_HISTORY) {
            return new PredictionResult(currentPrice, "محايد", 40);
        }

        int currentIndex = bars.size() - 1;
        if (currentPrice <= 0) {
            currentPrice = bars.get(currentIndex).close;
        }

        if (currentPrice <= 0) {
            return new PredictionResult(0, "محايد", 40);
        }

        // 1. حساب المؤشرات الفنية بدقة
        double ema5 = calculateEMA(bars, currentIndex, 5);
        double ema10 = calculateEMA(bars, currentIndex, 10);
        double ema20 = calculateEMA(bars, currentIndex, 20);
        double rsi = calculateRSI(bars, currentIndex, 14);
        double m1 = momentum(bars, currentIndex, 1);
        double m3 = momentum(bars, currentIndex, 3);
        double m5 = momentum(bars, currentIndex, 5);

        // 2. تحليل الاتجاه التوافقي (Confluence Scoring)
        int trendSignal = 0; // +1 صاعد, -1 هابط
        if (ema5 > ema10 && ema10 > ema20) trendSignal = 1;
        else if (ema5 < ema10 && ema10 < ema20) trendSignal = -1;

        int momentumSignal = 0;
        double avgMomentum = (m1 * 0.5) + (m3 * 0.3) + (m5 * 0.2);
        if (avgMomentum > THRESHOLD) momentumSignal = 1;
        else if (avgMomentum < -THRESHOLD) momentumSignal = -1;

        int rsiSignal = 0;
        if (rsi > 50 && rsi < 70) rsiSignal = 1;      // صاعد قوي
        else if (rsi < 50 && rsi > 30) rsiSignal = -1; // هابط قوي
        else if (rsi <= 30) rsiSignal = 1;            // تشبع بيعي (ارتداد محتمل)
        else if (rsi >= 70) rsiSignal = -1;           // تشبع شرائي (تصحيح محتمل)

        // 3. حساب الوزن الإجمالي للإشارات
        int totalScore = (trendSignal * 2) + (momentumSignal * 2) + rsiSignal;

        // 4. تحديد الاتجاه والتغير المتوقع
        String direction = "محايد";
        double expectedReturn = 0.0;

        if (totalScore >= 2) {
            direction = "صاعد";
            expectedReturn = Math.max(THRESHOLD, Math.abs(avgMomentum));
        } else if (totalScore <= -2) {
            direction = "هابط";
            expectedReturn = -Math.max(THRESHOLD, Math.abs(avgMomentum));
        } else {
            direction = "عرضي";
            expectedReturn = avgMomentum * 0.5;
        }

        // كبح الشذوذ في التوقع (Max 1.5% حركة يومية)
        expectedReturn = Math.max(-0.015, Math.min(0.015, expectedReturn));
        double predictedPrice = currentPrice * (1.0 + expectedReturn);

        // 5. حساب نسبة الثقة بناءً على الاتساق بين المؤشرات (Confluence)
        int matchedSignals = 0;
        int activeSignals = 3;

        if ((direction.equals("صاعد") && trendSignal > 0) || (direction.equals("هابط") && trendSignal < 0)) matchedSignals++;
        if ((direction.equals("صاعد") && momentumSignal > 0) || (direction.equals("هابط") && momentumSignal < 0)) matchedSignals++;
        if ((direction.equals("صاعد") && rsiSignal > 0) || (direction.equals("هابط") && rsiSignal < 0)) matchedSignals++;

        int confidence = 50 + (matchedSignals * 12);
        if (direction.equals("عرضي")) confidence = 45;

        // تعديل الثقة بناءً على متانة الاتجاه مع EMA20
        if (direction.equals("صاعد") && currentPrice > ema20) confidence += 8;
        if (direction.equals("هابط") && currentPrice < ema20) confidence += 8;

        confidence = Math.min(88, Math.max(35, confidence));

        return new PredictionResult(predictedPrice, direction, confidence);
    }

    // حساب EMA الدقيق والصحيح
    private double calculateEMA(List<HistoricalGoldProvider.GoldBar> bars, int index, int period) {
        if (index < period - 1) return bars.get(index).close;
        double multiplier = 2.0 / (period + 1);
        double ema = bars.get(index - period + 1).close; // البداية بـ Simple Average تقريبي

        for (int i = index - period + 2; i <= index; i++) {
            ema = ((bars.get(i).close - ema) * multiplier) + ema;
        }
        return ema;
    }

    // حساب RSI الصحيح
    private double calculateRSI(List<HistoricalGoldProvider.GoldBar> bars, int index, int period) {
        if (index < period) return 50.0;

        double gains = 0.0;
        double losses = 0.0;

        for (int i = index - period + 1; i <= index; i++) {
            double change = bars.get(i).close - bars.get(i - 1).close;
            if (change > 0) gains += change;
            else losses -= change;
        }

        if (losses == 0) return 100.0;
        double avgGain = gains / period;
        double avgLoss = losses / period;
        double rs = avgGain / avgLoss;

        return 100.0 - (100.0 / (1.0 + rs));
    }

    // حساب الزخم (Momentum)
    private double momentum(List<HistoricalGoldProvider.GoldBar> bars, int index, int days) {
        if (index < days) return 0.0;
        double current = bars.get(index).close;
        double previous = bars.get(index - days).close;
        if (previous == 0) return 0.0;
        return (current - previous) / previous;
    }
}
