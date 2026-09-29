package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredictionEngine {

    /*
     * حدود الحركة المستخدمة لتصنيف الاتجاه.
     */
    private static final double UP_THRESHOLD = 0.0030;
    private static final double DOWN_THRESHOLD = -0.0030;

    /*
     * أقل عدد بيانات مطلوب.
     */
    private static final int MIN_HISTORY = 20;

    /*
     * معامل لتقليل مبالغة السعر المتوقع.
     */
    private static final double CALIBRATION_FACTOR = 0.45;

    /*
     * الحد الأقصى للحركة المتوقعة.
     */
    private static final double MAX_FORECAST = 0.030;

    public PredictionResult analyze(
            double currentPrice,
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (
                bars == null ||
                bars.size() < MIN_HISTORY
        ) {

            return new PredictionResult(
                    currentPrice,
                    currentPrice,
                    "بيانات غير كافية",
                    0
            );
        }

        List<HistoricalGoldProvider.GoldBar> data =
                new ArrayList<>(bars);

        Collections.sort(
                data,
                (a, b) -> a.date.compareTo(b.date)
        );

        int currentIndex =
                data.size() - 1;

        /*
         * ==============================
         * المؤشرات الأساسية
         * ==============================
         */

        double ema5 =
                calculateEMA(
                        data,
                        currentIndex,
                        5
                );

        double ema10 =
                calculateEMA(
                        data,
                        currentIndex,
                        10
                );

        double ema20 =
                calculateEMA(
                        data,
                        currentIndex,
                        20
                );

        double rsi =
                calculateRSI(
                        data,
                        currentIndex,
                        14
                );

        double momentum1 =
                calculateMomentum(
                        data,
                        currentIndex,
                        1
                );

        double momentum3 =
                calculateMomentum(
                        data,
                        currentIndex,
                        3
                );

        double momentum5 =
                calculateMomentum(
                        data,
                        currentIndex,
                        5
                );

        /*
         * ==============================
         * نقاط الاتجاه
         * ==============================
         *
         * كل مجموعة مؤشرات تعطي إشارة
         * مستقلة بدل الاعتماد على مؤشر واحد.
         */

        double trendScore = 0;
        double momentumScore = 0;
        double rsiScore = 0;
        double positionScore = 0;

        /*
         * ==============================
         * 1) TREND
         * ==============================
         */

        if (ema5 > ema10) {
            trendScore += 1.5;
        } else {
            trendScore -= 1.5;
        }

        if (ema10 > ema20) {
            trendScore += 1.5;
        } else {
            trendScore -= 1.5;
        }

        /*
         * ==============================
         * 2) POSITION
         * ==============================
         */

        if (currentPrice > ema5) {
            positionScore += 1.0;
        } else {
            positionScore -= 1.0;
        }

        if (currentPrice > ema10) {
            positionScore += 0.75;
        } else {
            positionScore -= 0.75;
        }

        /*
         * ==============================
         * 3) MOMENTUM
         * ==============================
         */

        momentumScore +=
                momentumSignal(
                        momentum1,
                        1.0
                );

        momentumScore +=
                momentumSignal(
                        momentum3,
                        1.5
                );

        momentumScore +=
                momentumSignal(
                        momentum5,
                        2.0
                );

        /*
         * ==============================
         * 4) RSI
         * ==============================
         *
         * لا نعتبر RSI المرتفع وحده
         * إشارة هبوط مباشرة.
         */

        if (rsi >= 55 && rsi <= 68) {

            rsiScore += 1.5;

        } else if (rsi >= 45 && rsi < 55) {

            rsiScore += 0;

        } else if (rsi >= 32 && rsi < 45) {

            rsiScore -= 1.0;

        } else if (rsi < 32) {

            /*
             * تشبع بيعي قد يعني ارتداد.
             */
            rsiScore += 1.0;

        } else if (rsi > 68) {

            /*
             * تشبع شرائي.
             */
            rsiScore -= 1.0;
        }

        /*
         * ==============================
         * الدرجة النهائية للاتجاه
         * ==============================
         */

        double totalScore =
                trendScore +
                        momentumScore +
                        rsiScore +
                        positionScore;

        /*
         * ==============================
         * حساب التغير المتوقع
         * ==============================
         */

        double trendReturn =
                calculateTrendReturn(
                        currentPrice,
                        ema10
                );

        /*
         * الزخم له الوزن الأكبر.
         */
        double forecastReturn =
                momentum1 * 0.15 +
                        momentum3 * 0.25 +
                        momentum5 * 0.30 +
                        trendReturn * 0.20;

        /*
         * RSI adjustment.
         */
        if (rsi < 32) {

            forecastReturn += 0.0020;

        } else if (rsi > 68) {

            forecastReturn -= 0.0020;

        } else if (
                rsi >= 55 &&
                        rsi <= 68
        ) {

            forecastReturn += 0.0010;

        } else if (
                rsi >= 32 &&
                        rsi < 45
        ) {

            forecastReturn -= 0.0010;
        }

        /*
         * ==============================
         * تسارع الزخم
         * ==============================
         *
         * إذا كان زخم اليوم أقوى من متوسط
         * الزخم الأطول، نضيف دفعة صغيرة.
         */

        double momentumAcceleration =
                momentum1 -
                        (
                                momentum5 / 5.0
                        );

        forecastReturn +=
                momentumAcceleration * 0.15;

        /*
         * ==============================
         * توافق الإشارات
         * ==============================
         */

        int bullishVotes = 0;
        int bearishVotes = 0;

        if (trendScore > 0) {
            bullishVotes++;
        } else if (trendScore < 0) {
            bearishVotes++;
        }

        if (momentumScore > 0) {
            bullishVotes++;
        } else if (momentumScore < 0) {
            bearishVotes++;
        }

        if (rsiScore > 0) {
            bullishVotes++;
        } else if (rsiScore < 0) {
            bearishVotes++;
        }

        if (positionScore > 0) {
            bullishVotes++;
        } else if (positionScore < 0) {
            bearishVotes++;
        }

        /*
         * ==============================
         * تقليل الحركة عندما يكون هناك
         * تعارض واضح بين المؤشرات.
         * ==============================
         */

        int voteDifference =
                Math.abs(
                        bullishVotes -
                                bearishVotes
                );

        if (voteDifference <= 1) {

            forecastReturn *= 0.60;

        } else if (voteDifference == 2) {

            forecastReturn *= 0.85;

        } else {

            forecastReturn *= 1.05;
        }

        /*
         * ==============================
         * معايرة حجم التوقع
         * ==============================
         */

        forecastReturn *=
                CALIBRATION_FACTOR;

        /*
         * ==============================
         * الحد الأقصى
         * ==============================
         */

        if (
                forecastReturn >
                        MAX_FORECAST
        ) {

            forecastReturn =
                    MAX_FORECAST;

        }

        if (
                forecastReturn <
                        -MAX_FORECAST
        ) {

            forecastReturn =
                    -MAX_FORECAST;
        }

        /*
         * ==============================
         * تحديد الاتجاه
         * ==============================
         *
         * نستخدم الاتجاه المركب وليس السعر
         * المتوقع وحده.
         */

        String direction;

        /*
         * إذا كانت الإشارات متقاربة جدًا،
         * نعتبرها عرضية.
         */
        if (
                voteDifference <= 1 &&
                        Math.abs(totalScore) < 2.5
        ) {

            direction = "عرضي ↔";

        } else if (
                totalScore >= 3.0 &&
                        forecastReturn >=
                                UP_THRESHOLD
        ) {

            direction = "صعود ↑";

        } else if (
                totalScore <= -3.0 &&
                        forecastReturn <=
                                DOWN_THRESHOLD
        ) {

            direction = "هبوط ↓";

        } else {

            /*
             * لو الاتجاه قوي لكن مقدار الحركة
             * صغير، نسمح للدرجة المركبة
             * بتحديد الاتجاه.
             */

            if (totalScore >= 4.0) {

                direction = "صعود ↑";

            } else if (totalScore <= -4.0) {

                direction = "هبوط ↓";

            } else {

                direction = "عرضي ↔";
            }
        }

        /*
         * ==============================
         * السعر المتوقع
         * ==============================
         */

        double predictedPrice =
                currentPrice *
                        (
                                1.0 +
                                        forecastReturn
                        );

        /*
         * ==============================
         * الثقة
         * ==============================
         */

        double confidence =
                calculateConfidence(
                        totalScore,
                        bullishVotes,
                        bearishVotes,
                        voteDifference,
                        rsi,
                        forecastReturn
                );

        return new PredictionResult(
                currentPrice,
                predictedPrice,
                direction,
                confidence
        );
    }

    /*
     * ==============================
     * إشارة الزخم
     * ==============================
     */

    private double momentumSignal(
            double momentum,
            double weight) {

        if (momentum > 0.0030) {

            return weight;

        } else if (momentum < -0.0030) {

            return -weight;

        } else {

            return momentum *
                    weight /
                    0.0030;
        }
    }

    /*
     * ==============================
     * Trend Return
     * ==============================
     */

    private double calculateTrendReturn(
            double currentPrice,
            double ema10) {

        if (ema10 == 0) {
            return 0;
        }

        return (
                currentPrice -
                        ema10
        ) / ema10;
    }

    /*
     * ==============================
     * EMA
     * ==============================
     */

    private double calculateEMA(
            List<HistoricalGoldProvider.GoldBar> bars,
            int endIndex,
            int period) {

        if (
                bars == null ||
                        bars.isEmpty() ||
                        endIndex < 0
        ) {

            return 0;
        }

        int count =
                Math.min(
                        period,
                        endIndex + 1
                );

        int start =
                endIndex -
                        count +
                        1;

        double ema =
                bars.get(start).close;

        double multiplier =
                2.0 /
                        (count + 1.0);

        for (
                int i = start + 1;
                i <= endIndex;
                i++
        ) {

            double close =
                    bars.get(i).close;

            ema =
                    (
                            close -
                                    ema
                    )
                            * multiplier
                            + ema;
        }

        return ema;
    }

    /*
     * ==============================
     * RSI
     * ==============================
     */

    private double calculateRSI(
            List<HistoricalGoldProvider.GoldBar> bars,
            int endIndex,
            int period) {

        if (
                bars == null ||
                        endIndex < 1
        ) {

            return 50;
        }

        int available =
                endIndex;

        period =
                Math.min(
                        period,
                        available
                );

        if (period <= 0) {

            return 50;
        }

        double gains = 0;
        double losses = 0;

        int start =
                endIndex -
                        period +
                        1;

        for (
                int i = start;
                i <= endIndex;
                i++
        ) {

            double current =
                    bars.get(i).close;

            double previous =
                    bars.get(i - 1).close;

            double change =
                    current -
                            previous;

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
     * ==============================
     * Momentum
     * ==============================
     */

    private double calculateMomentum(
            List<HistoricalGoldProvider.GoldBar> bars,
            int endIndex,
            int period) {

        if (
                endIndex < period
        ) {

            return 0;
        }

        double latest =
                bars.get(
                        endIndex
                ).close;

        double previous =
                bars.get(
                        endIndex -
                                period
                ).close;

        if (previous == 0) {
            return 0;
        }

        return (
                latest -
                        previous
        ) / previous;
    }

    /*
     * ==============================
     * Confidence
     * ==============================
     */

    private double calculateConfidence(
            double totalScore,
            int bullishVotes,
            int bearishVotes,
            int voteDifference,
            double rsi,
            double forecastReturn) {

        double confidence = 35;

        /*
         * قوة الاتجاه.
         */

        double scoreStrength =
                Math.min(
                        Math.abs(totalScore),
                        8.0
                );

        confidence +=
                scoreStrength *
                        4.0;

        /*
         * اتفاق المؤشرات.
         */

        if (voteDifference >= 3) {

            confidence += 15;

        } else if (voteDifference == 2) {

            confidence += 8;

        } else if (voteDifference == 1) {

            confidence -= 5;
        }

        /*
         * قوة الحركة المتوقعة.
         */

        double movement =
                Math.abs(
                        forecastReturn
                );

        if (movement >= 0.010) {

            confidence += 8;

        } else if (movement >= 0.005) {

            confidence += 4;
        }

        /*
         * RSI المتطرف يقلل الثقة قليلًا
         * لأن احتمالية الانعكاس تزيد.
         */

        if (
                rsi < 25 ||
                        rsi > 75
        ) {

            confidence -= 5;
        }

        /*
         * الحدود.
         */

        if (confidence > 85) {
            confidence = 85;
        }

        if (confidence < 20) {
            confidence = 20;
        }

        return confidence;
    }

    /*
     * ==============================
     * Prediction Result
     * ==============================
     */

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

            this.currentPrice =
                    currentPrice;

            this.predictedPrice =
                    predictedPrice;

            this.direction =
                    direction;

            this.confidence =
                    confidence;
        }
    }
}
