package com.goldai.app.data;

import java.util.List;

public class PredictionEngine {

    public static class PredictionResult {
        public double predictedPrice;
        public String direction;
        public double confidence;

        public PredictionResult(double predictedPrice,
                                String direction,
                                double confidence) {
            this.predictedPrice = predictedPrice;
            this.direction = direction;
            this.confidence = confidence;
        }
    }

    /**
     * يتوقع الاتجاه أولاً، ثم يحدد حجم الحركة بشكل منفصل.
     *
     * الفكرة:
     * 1- الاتجاه يعتمد على الزخم والمتوسطات.
     * 2- حجم الحركة يعتمد على متوسط الحركة الأخيرة.
     * 3- لا نسمح لحجم الحركة بأن يغيّر الاتجاه.
     */
    public static PredictionResult predict(List<GoldBar> bars) {

        if (bars == null || bars.size() < 10) {
            return new PredictionResult(
                    bars != null && !bars.isEmpty()
                            ? bars.get(bars.size() - 1).close
                            : 0.0,
                    "SIDEWAYS",
                    50.0
            );
        }

        int n = bars.size();

        double current = bars.get(n - 1).close;
        double previous = bars.get(n - 2).close;

        // =========================
        // 1) حساب التغيرات الأخيرة
        // =========================

        double change1 = current - previous;

        double change3 = 0.0;
        if (n >= 4) {
            change3 = current - bars.get(n - 4).close;
        }

        double change5 = 0.0;
        if (n >= 6) {
            change5 = current - bars.get(n - 6).close;
        }

        // =========================
        // 2) المتوسطات المتحركة
        // =========================

        double ma5 = movingAverage(bars, 5);
        double ma10 = movingAverage(bars, 10);

        // =========================
        // 3) تحديد الاتجاه فقط
        // =========================

        double directionScore = 0.0;

        // السعر فوق MA5
        if (current > ma5) {
            directionScore += 1.0;
        } else if (current < ma5) {
            directionScore -= 1.0;
        }

        // MA5 فوق MA10
        if (ma5 > ma10) {
            directionScore += 1.0;
        } else if (ma5 < ma10) {
            directionScore -= 1.0;
        }

        // حركة آخر 3 شموع
        if (change3 > 0) {
            directionScore += 1.0;
        } else if (change3 < 0) {
            directionScore -= 1.0;
        }

        // حركة آخر 5 شموع ولكن بوزن أقل
        if (change5 > 0) {
            directionScore += 0.5;
        } else if (change5 < 0) {
            directionScore -= 0.5;
        }

        // الحركة اللحظية لها وزن صغير فقط
        if (change1 > 0) {
            directionScore += 0.25;
        } else if (change1 < 0) {
            directionScore -= 0.25;
        }

        // =========================
        // 4) الاتجاه النهائي
        // =========================

        String direction;

        if (directionScore >= 1.5) {
            direction = "UP";
        } else if (directionScore <= -1.5) {
            direction = "DOWN";
        } else {
            direction = "SIDEWAYS";
        }

        // =========================
        // 5) حساب متوسط حجم الحركة
        // =========================

        double averageMove = averageAbsoluteMove(bars, 10);

        // منع الحركة من أن تصبح كبيرة بشكل مبالغ فيه
        double maxMove = current * 0.025; // 2.5%
        double minMove = current * 0.001;  // 0.1%

        double moveSize = averageMove;

        if (moveSize < minMove) {
            moveSize = minMove;
        }

        if (moveSize > maxMove) {
            moveSize = maxMove;
        }

        // =========================
        // 6) فصل الاتجاه عن حجم الحركة
        // =========================

        double predictedPrice;

        if ("UP".equals(direction)) {

            predictedPrice = current + moveSize;

        } else if ("DOWN".equals(direction)) {

            predictedPrice = current - moveSize;

        } else {

            // في حالة الاتجاه الجانبي،
            // لا نقفز بالسعر بقوة.
            predictedPrice = current;
        }

        // =========================
        // 7) حساب الثقة
        // =========================

        double confidence;

        double absScore = Math.abs(directionScore);

        if ("SIDEWAYS".equals(direction)) {

            confidence = 50.0 + Math.min(absScore * 5.0, 10.0);

        } else {

            confidence = 50.0 + Math.min(absScore * 8.0, 40.0);
        }

        // لا نسمح بثقة أقل من 50 أو أعلى من 90
        confidence = Math.max(50.0, confidence);
        confidence = Math.min(90.0, confidence);

        return new PredictionResult(
                predictedPrice,
                direction,
                confidence
        );
    }

    /**
     * حساب Moving Average
     */
    private static double movingAverage(List<GoldBar> bars, int period) {

        int n = bars.size();

        if (n == 0) {
            return 0.0;
        }

        int start = Math.max(0, n - period);

        double sum = 0.0;
        int count = 0;

        for (int i = start; i < n; i++) {
            sum += bars.get(i).close;
            count++;
        }

        return count > 0 ? sum / count : bars.get(n - 1).close;
    }

    /**
     * متوسط حجم الحركة المطلقة في آخر عدد من الشموع.
     */
    private static double averageAbsoluteMove(List<GoldBar> bars, int period) {

        int n = bars.size();

        if (n < 2) {
            return 0.0;
        }

        int start = Math.max(1, n - period);

        double sum = 0.0;
        int count = 0;

        for (int i = start; i < n; i++) {

            double move =
                    Math.abs(bars.get(i).close - bars.get(i - 1).close);

            sum += move;
            count++;
        }

        return count > 0 ? sum / count : 0.0;
    }
}
