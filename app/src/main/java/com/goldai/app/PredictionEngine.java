package com.goldai.app;

import com.goldai.app.data.HistoricalGoldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredictionEngine {

    /*
     * حد تصنيف الاتجاه:
     *
     * +0.30% أو أكثر = صعود
     * -0.30% أو أقل = هبوط
     * بينهما        = عرضي
     */
    private static final double DIRECTION_THRESHOLD = 0.0030;

    public PredictionResult analyze(
            double currentPrice,
            List<HistoricalGoldProvider.GoldBar> bars) {

        if (bars == null || bars.size() < 15) {

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
         * =========================
         * 1. المؤشرات الأساسية
         * =========================
         */

        double ema5 =
                calculateEMA(data, 5);

        double ema10 =
                calculateEMA(data, 10);

        double ema20 =
                calculateEMA(data, 20);

        double ema30 =
                calculateEMA(data, 30);

        double rsi =
                calculateRSI(data, 14);

        double momentum1 =
                calculateMomentum(data, 1);

        double momentum3 =
                calculateMomentum(data, 3);

        double momentum5 =
                calculateMomentum(data, 5);

        double momentum10 =
                calculateMomentum(data, 10);

        double volatility =
                calculateVolatility(data, 14);

        /*
         * =========================
         * 2. الاتجاه العام
         * =========================
         */

        double trendShort =
                calculateReturn(
                        ema5,
                        ema10
                );

        double trendMedium =
                calculateReturn(
                        ema10,
                        ema20
                );

        double trendLong =
                calculateReturn(
                        ema20,
                        ema30
                );

        /*
         * ميل آخر 10 أيام
         */
        double slope10 =
                calculateSlope(
                        data,
                        10
                );

        /*
         * =========================
         * 3. حساب الزخم المركب
         * =========================
         */

        double momentumScore =
                (
                        momentum1 * 0.10
                                +
                        momentum3 * 0.25
                                +
                        momentum5 * 0.35
                                +
                        momentum10 * 0.30
                );

        /*
         * =========================
         * 4. حساب الاتجاه المركب
         * =========================
         */

        double trendScore =
                (
                        trendShort * 0.40
                                +
                        trendMedium * 0.35
                                +
                        trendLong * 0.25
                );

        /*
         * =========================
         * 5. تأثير السعر الحالي
         * =========================
         */

        double priceVsEma20 =
                calculateReturn(
                        currentPrice,
                        ema20
                );

        /*
         * =========================
         * 6. RSI
         * =========================
         *
         * لا نتعامل مع RSI وحده.
         * نستخدمه كعامل تصحيح.
         */

        double rsiAdjustment = 0;

        if (rsi >= 55 && rsi < 70) {

            rsiAdjustment = 0.0015;

        } else if (rsi >= 70) {

            /*
             * تشبع شرائي:
             * نقلل التوقع الصاعد.
             */
            rsiAdjustment = -0.0015;

        } else if (rsi > 30 && rsi <= 45) {

            rsiAdjustment = -0.0010;

        } else if (rsi <= 30) {

            /*
             * تشبع بيعي:
             * احتمال ارتداد.
             */
            rsiAdjustment = 0.0015;
        }

        /*
         * =========================
         * 7. دمج الإشارات
         * =========================
         */

        double forecastReturn =
                (
                        momentumScore * 0.45
                                +
                        trendScore * 0.30
                                +
                        priceVsEma20 * 0.10
                                +
                        slope10 * 0.15
                );

        /*
         * إضافة تأثير RSI.
         */
        forecastReturn +=
                rsiAdjustment;

        /*
         * =========================
         * 8. فلتر التذبذب
         * =========================
         *
         * في الأسواق شديدة التذبذب
         * نقلل حجم التوقع بدل تضخيمه.
         */

        if (volatility > 0.035) {

            forecastReturn *= 0.90;
        }

        if (volatility > 0.050) {

            forecastReturn *= 0.80;
        }

        if (volatility > 0.070) {

            forecastReturn *= 0.70;
        }

        /*
         * =========================
         * 9. الحد الأقصى للتوقع
         * =========================
         *
         * حتى لا يعطي النموذج قفزات
         * غير منطقية.
         */

        if (forecastReturn > 0.025) {

            forecastReturn = 0.025;
        }

        if (forecastReturn < -0.025) {

            forecastReturn = -0.025;
        }

        /*
         * =========================
         * 10. السعر المتوقع
         * =========================
         */

        double predictedPrice =
                currentPrice *
                        (1.0 + forecastReturn);

        /*
         * =========================
         * 11. تحديد الاتجاه
         * =========================
         *
         * أصبح متوافقًا مع Backtest:
         *
         * >= +0.30% صعود
         * <= -0.30% هبوط
         * غير ذلك عرضي
         */

        String direction;

        if (forecastReturn >=
                DIRECTION_THRESHOLD) {

            direction =
                    "صعود ↑";

        } else if (forecastReturn <=
                -DIRECTION_THRESHOLD) {

            direction =
                    "هبوط ↓";

        } else {

            direction =
                    "عرضي ↔";
        }

        /*
         * =========================
         * 12. حساب قوة الإشارات
         * =========================
         */

        double agreement =
                calculateAgreement(
                        momentum1,
                        momentum3,
                        momentum5,
                        momentum10,
                        trendShort,
                        trendMedium,
                        trendLong
                );

        /*
         * =========================
         * 13. الثقة
         * =========================
         */

        double confidence =
                calculateConfidence(
                        direction,
                        forecastReturn,
                        rsi,
                        volatility,
                        agreement
                );

        return new PredictionResult(
                currentPrice,
                predictedPrice,
                direction,
                confidence
        );
    }

    /*
     * =========================
     * EMA
     * =========================
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
                2.0 / (count + 1.0);

        for (
                int i = start + 1;
                i < bars.size();
                i++
        ) {

            double close =
                    bars.get(i).close;

            ema =
                    (
                            (close - ema)
                                    * multiplier
                    )
                            + ema;
        }

        return ema;
    }

    /*
     * =========================
     * RSI
     * =========================
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
     * =========================
     * Momentum
     * =========================
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
     * =========================
     * Return
     * =========================
     */

    private double calculateReturn(
            double current,
            double previous) {

        if (previous == 0) {

            return 0;
        }

        return (
                current - previous
        ) / previous;
    }

    /*
     * =========================
     * ميل السعر
     * =========================
     */

    private double calculateSlope(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        if (bars == null ||
                bars.size() < 2) {

            return 0;
        }

        int count =
                Math.min(
                        period,
                        bars.size()
                );

        int start =
                bars.size() - count;

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (
                int i = 0;
                i < count;
                i++
        ) {

            double x = i;

            double y =
                    bars.get(
                            start + i
                    ).close;

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double denominator =
                count * sumXX
                        - sumX * sumX;

        if (denominator == 0) {

            return 0;
        }

        double slope =
                (
                        count * sumXY
                                - sumX * sumY
                )
                        / denominator;

        double average =
                sumY / count;

        if (average == 0) {

            return 0;
        }

        /*
         * نحول الميل إلى نسبة تقريبية
         * خلال الفترة.
         */

        return slope *
                (count - 1)
                / average;
    }

    /*
     * =========================
     * Volatility
     * =========================
     */

    private double calculateVolatility(
            List<HistoricalGoldProvider.GoldBar> bars,
            int period) {

        int count =
                Math.min(
                        period,
                        bars.size()
                );

        if (count < 2) {

            return 0;
        }

        /*
         * نحسب التذبذب من العوائد اليومية
         * بدل فرق الأسعار الخام.
         */

        int start =
                bars.size() - count;

        double sumReturns = 0;

        int returnCount = 0;

        for (
                int i = start + 1;
                i < bars.size();
                i++
        ) {

            double previous =
                    bars.get(i - 1).close;

            double current =
                    bars.get(i).close;

            if (previous == 0) {

                continue;
            }

            double dailyReturn =
                    (
                            current - previous
                    ) / previous;

            sumReturns +=
                    dailyReturn;

            returnCount++;
        }

        if (returnCount < 2) {

            return 0;
        }

        double averageReturn =
                sumReturns /
                        returnCount;

        double variance = 0;

        for (
                int i = start + 1;
                i < bars.size();
                i++
        ) {

            double previous =
                    bars.get(i - 1).close;

            double current =
                    bars.get(i).close;

            if (previous == 0) {

                continue;
            }

            double dailyReturn =
                    (
                            current - previous
                    ) / previous;

            double difference =
                    dailyReturn
                            - averageReturn;

            variance +=
                    difference *
                            difference;
        }

        variance /=
                returnCount;

        return Math.sqrt(
                variance
        );
    }

    /*
     * =========================
     * اتفاق المؤشرات
     * =========================
     */

    private double calculateAgreement(
            double momentum1,
            double momentum3,
            double momentum5,
            double momentum10,
            double trendShort,
            double trendMedium,
            double trendLong) {

        int positive = 0;
        int negative = 0;

        double[] values = {

                momentum1,
                momentum3,
                momentum5,
                momentum10,

                trendShort,
                trendMedium,
                trendLong
        };

        for (double value : values) {

            if (value > 0.001) {

                positive++;

            } else if (value < -0.001) {

                negative++;
            }
        }

        int total =
                positive + negative;

        if (total == 0) {

            return 0;
        }

        return Math.abs(
                positive - negative
        ) / (double) total;
    }

    /*
     * =========================
     * Confidence
     * =========================
     */

    private double calculateConfidence(
            String direction,
            double forecastReturn,
            double rsi,
            double volatility,
            double agreement) {

        double confidence = 50;

        /*
         * قوة الإشارة المتوقعة.
         */

        double magnitude =
                Math.abs(
                        forecastReturn
                );

        if (magnitude >= 0.015) {

            confidence += 12;

        } else if (magnitude >= 0.010) {

            confidence += 9;

        } else if (magnitude >= 0.005) {

            confidence += 5;

        } else if (magnitude < 0.002) {

            confidence -= 5;
        }

        /*
         * اتفاق المؤشرات.
         */

        confidence +=
                agreement * 12;

        /*
         * التذبذب العالي يقلل الثقة.
         */

        if (volatility > 0.035) {

            confidence -= 5;
        }

        if (volatility > 0.050) {

            confidence -= 7;
        }

        if (volatility > 0.070) {

            confidence -= 8;
        }

        /*
         * RSI شديد التطرف
         * يقلل الثقة قليلًا.
         */

        if (rsi >= 75 ||
                rsi <= 25) {

            confidence -= 5;
        }

        /*
         * العرضي بطبيعته أقل ثقة
         * عندما تكون الإشارة ضعيفة.
         */

        if (direction.equals(
                "عرضي ↔"
        )) {

            confidence -= 3;
        }

        /*
         * الحدود النهائية.
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
     * =========================
     * Prediction Result
     * =========================
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
