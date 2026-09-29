package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PredictionEngine {

    /*
     * عدد الحالات التاريخية الأقرب التي سيتم استخدامها.
     */
    private static final int NEIGHBORS = 8;

    /*
     * أقل عدد من البيانات المطلوبة.
     */
    private static final int MIN_HISTORY = 20;

    /*
     * حدود تحديد الاتجاه.
     */
    private static final double UP_THRESHOLD = 0.0030;
    private static final double DOWN_THRESHOLD = -0.0030;

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

        /*
         * نستخدم آخر سعر في التاريخ كالحالة الحالية.
         */
        int currentIndex =
                data.size() - 1;

        /*
         * ==============================
         * مؤشرات الحالة الحالية
         * ==============================
         */

        double currentEma5 =
                calculateEMA(
                        data,
                        currentIndex,
                        5
                );

        double currentEma10 =
                calculateEMA(
                        data,
                        currentIndex,
                        10
                );

        double currentEma20 =
                calculateEMA(
                        data,
                        currentIndex,
                        20
                );

        double currentRsi =
                calculateRSI(
                        data,
                        currentIndex,
                        14
                );

        double currentMomentum1 =
                calculateMomentum(
                        data,
                        currentIndex,
                        1
                );

        double currentMomentum3 =
                calculateMomentum(
                        data,
                        currentIndex,
                        3
                );

        double currentMomentum5 =
                calculateMomentum(
                        data,
                        currentIndex,
                        5
                );

        /*
         * ==============================
         * البحث عن الحالات المشابهة
         * ==============================
         *
         * لا نسمح باستخدام اليوم الحالي
         * أو أي يوم بعده.
         *
         * وكل حالة تاريخية يجب أن يكون
         * لها يوم تالٍ معروف.
         */

        List<Neighbor> neighbors =
                new ArrayList<>();

        int firstCandidate =
                14;

        int lastCandidate =
                currentIndex - 1;

        for (
                int i = firstCandidate;
                i <= lastCandidate;
                i++
        ) {

            double ema5 =
                    calculateEMA(
                            data,
                            i,
                            5
                    );

            double ema10 =
                    calculateEMA(
                            data,
                            i,
                            10
                    );

            double ema20 =
                    calculateEMA(
                            data,
                            i,
                            20
                    );

            double rsi =
                    calculateRSI(
                            data,
                            i,
                            14
                    );

            double momentum1 =
                    calculateMomentum(
                            data,
                            i,
                            1
                    );

            double momentum3 =
                    calculateMomentum(
                            data,
                            i,
                            3
                    );

            double momentum5 =
                    calculateMomentum(
                            data,
                            i,
                            5
                    );

            double historicalPrice =
                    data.get(i).close;

            double nextPrice =
                    data.get(i + 1).close;

            if (historicalPrice == 0) {
                continue;
            }

            double nextReturn =
                    (
                            nextPrice -
                                    historicalPrice
                    ) / historicalPrice;

            /*
             * ==============================
             * المسافة بين الحالة الحالية
             * والحالة التاريخية
             * ==============================
             */

            double distance =
                    calculateDistance(
                            currentPrice,
                            currentEma5,
                            currentEma10,
                            currentEma20,
                            currentRsi,
                            currentMomentum1,
                            currentMomentum3,
                            currentMomentum5,

                            historicalPrice,
                            ema5,
                            ema10,
                            ema20,
                            rsi,
                            momentum1,
                            momentum3,
                            momentum5
                    );

            neighbors.add(
                    new Neighbor(
                            distance,
                            nextReturn
                    )
            );
        }

        /*
         * ترتيب الحالات من الأكثر تشابهًا
         * إلى الأقل تشابهًا.
         */

        Collections.sort(
                neighbors,
                (a, b) ->
                        Double.compare(
                                a.distance,
                                b.distance
                        )
        );

        /*
         * ==============================
         * أخذ أقرب الحالات
         * ==============================
         */

        int neighborCount =
                Math.min(
                        NEIGHBORS,
                        neighbors.size()
                );

        if (neighborCount == 0) {

            return new PredictionResult(
                    currentPrice,
                    currentPrice,
                    "عرضي ↔",
                    20
            );
        }

        /*
         * ==============================
         * المتوسط المرجح
         * ==============================
         *
         * الحالة الأقرب لها وزن أكبر.
         */

        double weightedReturn = 0;
        double totalWeight = 0;

        int historicalUp = 0;
        int historicalDown = 0;
        int historicalSideways = 0;

        for (
                int i = 0;
                i < neighborCount;
                i++
        ) {

            Neighbor neighbor =
                    neighbors.get(i);

            double weight =
                    1.0 /
                            (
                                    0.001 +
                                            neighbor.distance
                            );

            weightedReturn +=
                    neighbor.nextReturn *
                            weight;

            totalWeight +=
                    weight;

            if (
                    neighbor.nextReturn
                            >= UP_THRESHOLD
            ) {

                historicalUp++;

            } else if (
                    neighbor.nextReturn
                            <= DOWN_THRESHOLD
            ) {

                historicalDown++;

            } else {

                historicalSideways++;
            }
        }

        double predictedReturn;

        if (totalWeight == 0) {

            predictedReturn = 0;

        } else {

            predictedReturn =
                    weightedReturn /
                            totalWeight;
        }

        /*
         * ==============================
         * تقليل المبالغة
         * ==============================
         *
         * الحالات التاريخية قد تحتوي على
         * حركة كبيرة جدًا.
         *
         * لذلك نستخدم جزءًا من متوسط الحركة
         * بدل نسخها بالكامل.
         */

        predictedReturn *= 0.65;

        /*
         * حدود أمان للحركة المتوقعة.
         */

        if (predictedReturn > 0.025) {

            predictedReturn = 0.025;
        }

        if (predictedReturn < -0.025) {

            predictedReturn = -0.025;
        }

        /*
         * ==============================
         * تحديد الاتجاه
         * ==============================
         */

        String direction;

        if (
                predictedReturn
                        >= UP_THRESHOLD
        ) {

            direction = "صعود ↑";

        } else if (
                predictedReturn
                        <= DOWN_THRESHOLD
        ) {

            direction = "هبوط ↓";

        } else {

            direction = "عرضي ↔";
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
                                        predictedReturn
                        );

        /*
         * ==============================
         * الثقة
         * ==============================
         */

        double confidence =
                calculateConfidence(
                        predictedReturn,
                        historicalUp,
                        historicalDown,
                        historicalSideways,
                        neighborCount
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
     * حساب المسافة
     * ==============================
     */

    private double calculateDistance(
            double currentPrice,
            double currentEma5,
            double currentEma10,
            double currentEma20,
            double currentRsi,
            double currentMomentum1,
            double currentMomentum3,
            double currentMomentum5,

            double historicalPrice,
            double historicalEma5,
            double historicalEma10,
            double historicalEma20,
            double historicalRsi,
            double historicalMomentum1,
            double historicalMomentum3,
            double historicalMomentum5) {

        /*
         * بدل مقارنة الأسعار الخام،
         * نقارن النسب حتى لا يصبح السعر نفسه
         * هو العامل المسيطر.
         */

        double currentEma5Gap =
                safeRatio(
                        currentPrice,
                        currentEma5
                );

        double historicalEma5Gap =
                safeRatio(
                        historicalPrice,
                        historicalEma5
                );

        double currentEma10Gap =
                safeRatio(
                        currentPrice,
                        currentEma10
                );

        double historicalEma10Gap =
                safeRatio(
                        historicalPrice,
                        historicalEma10
                );

        double currentEma20Gap =
                safeRatio(
                        currentPrice,
                        currentEma20
                );

        double historicalEma20Gap =
                safeRatio(
                        historicalPrice,
                        historicalEma20
                );

        /*
         * أوزان المؤشرات.
         */

        double ema5Difference =
                currentEma5Gap -
                        historicalEma5Gap;

        double ema10Difference =
                currentEma10Gap -
                        historicalEma10Gap;

        double ema20Difference =
                currentEma20Gap -
                        historicalEma20Gap;

        double rsiDifference =
                (
                        currentRsi -
                                historicalRsi
                ) / 100.0;

        double momentum1Difference =
                currentMomentum1 -
                        historicalMomentum1;

        double momentum3Difference =
                currentMomentum3 -
                        historicalMomentum3;

        double momentum5Difference =
                currentMomentum5 -
                        historicalMomentum5;

        /*
         * المسافة المربعة.
         */

        double distanceSquared =

                ema5Difference *
                        ema5Difference *
                        2.0

                        +

                ema10Difference *
                        ema10Difference *
                        2.0

                        +

                ema20Difference *
                        ema20Difference *
                        1.5

                        +

                rsiDifference *
                        rsiDifference *
                        1.5

                        +

                momentum1Difference *
                        momentum1Difference *
                        1.0

                        +

                momentum3Difference *
                        momentum3Difference *
                        1.5

                        +

                momentum5Difference *
                        momentum5Difference *
                        1.5;

        return Math.sqrt(
                distanceSquared
        );
    }

    /*
     * ==============================
     * نسبة آمنة
     * ==============================
     */

    private double safeRatio(
            double price,
            double average) {

        if (average == 0) {

            return 0;
        }

        return (
                price - average
        ) / average;
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
                endIndex - count + 1;

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
                            close - ema
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
                endIndex - period + 1;

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
                        endIndex - period
                ).close;

        if (previous == 0) {

            return 0;
        }

        return (
                latest - previous
        ) / previous;
    }

    /*
     * ==============================
     * Confidence
     * ==============================
     */

    private double calculateConfidence(
            double predictedReturn,
            int up,
            int down,
            int sideways,
            int total) {

        if (total <= 0) {

            return 20;
        }

        int dominant =
                Math.max(
                        up,
                        Math.max(
                                down,
                                sideways
                        )
                );

        double agreement =
                (
                        dominant * 100.0
                ) / total;

        double confidence =
                35;

        /*
         * اتفاق الحالات التاريخية.
         */

        if (agreement >= 75) {

            confidence += 25;

        } else if (agreement >= 62.5) {

            confidence += 15;

        } else if (agreement >= 50) {

            confidence += 7;
        }

        /*
         * قوة الحركة المتوقعة.
         */

        double strength =
                Math.abs(
                        predictedReturn
                );

        if (strength >= 0.010) {

            confidence += 10;

        } else if (strength >= 0.005) {

            confidence += 5;
        }

        /*
         * حدود الثقة.
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
     * Neighbor
     * ==============================
     */

    private static class Neighbor {

        double distance;
        double nextReturn;

        Neighbor(
                double distance,
                double nextReturn) {

            this.distance =
                    distance;

            this.nextReturn =
                    nextReturn;
        }
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
