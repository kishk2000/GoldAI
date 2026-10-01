package com.goldai.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.goldai.app.data.HistoricalGoldProvider;
import com.goldai.app.data.MarketData;
import com.goldai.app.data.MarketDataEngine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView priceUsd;
    private TextView egpUsd;
    private TextView egp24;
    private TextView egp21;
    private TextView egp18;

    private TextView prediction;
    private TextView confidence;
    private TextView backtest;
    private TextView status;

    private MarketDataEngine dataEngine;
    private HistoricalGoldProvider historyProvider;
    private PredictionEngine predictionEngine;
    private BacktestEngine backtestEngine;

    private List<HistoricalGoldProvider.GoldBar> historicalBars;
    private boolean historyLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        predictionEngine = new PredictionEngine();
        backtestEngine = new BacktestEngine();
        historyProvider = new HistoricalGoldProvider();

        loadHistoryOnce();

        dataEngine = new MarketDataEngine(new MarketDataEngine.DataCallback() {
            @Override
            public void onDataUpdated(MarketData data) {
                runOnUiThread(() -> {
                    updateMarketUI(data);

                    if (historyLoaded && historicalBars != null && !historicalBars.isEmpty()) {
                        runPrediction(data);
                    } else {
                        status.setText("⏳ جاري انتظار كتمال البيانات التاريخية...");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> status.setText("🔴 خطأ في السوق: " + error));
            }
        });

        dataEngine.startAutoUpdate();
    }

    private void initViews() {
        priceUsd = findViewById(R.id.txt_price_usd);
        egpUsd = findViewById(R.id.txt_egp_usd);
        egp24 = findViewById(R.id.txt_egp_24);
        egp21 = findViewById(R.id.txt_egp_21);
        egp18 = findViewById(R.id.txt_egp_18);

        prediction = findViewById(R.id.txt_prediction);
        confidence = findViewById(R.id.txt_confidence);
        backtest = findViewById(R.id.txt_backtest);
        status = findViewById(R.id.txt_status);
    }

    private void updateMarketUI(MarketData data) {
        if (data == null) return;

        priceUsd.setText(String.format(Locale.US, "$%.2f", data.goldUsd));
        egpUsd.setText(String.format(Locale.US, "الدولار: %.3f جنيه", data.usdEgp));
        egp24.setText(String.format(Locale.US, "عيار 24  %.0f جنيه", data.gold24Egp));
        egp21.setText(String.format(Locale.US, "عيار 21  %.0f جنيه", data.gold21Egp));
        egp18.setText(String.format(Locale.US, "عيار 18  %.0f جنيه", data.gold18Egp));
    }

    private void loadHistoryOnce() {
        historyProvider.getLatestHistory(new HistoricalGoldProvider.Callback() {
            @Override
            public void onSuccess(List<HistoricalGoldProvider.GoldBar> bars) {
                if (bars == null || bars.isEmpty()) {
                    runOnUiThread(() -> {
                        historyLoaded = false;
                        status.setText("🟡 القائمة التاريخية فارغة");
                    });
                    return;
                }

                historicalBars = bars;
                historyLoaded = true;

                runOnUiThread(() -> {
                    status.setText("🟢 تم تحميل " + historicalBars.size() + " شمعة تاريخية");
                    if (dataEngine != null && dataEngine.getLastData() != null) {
                        runPrediction(dataEngine.getLastData());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    historyLoaded = false;
                    prediction.setText("تعذر تحميل البيانات التاريخية");
                    confidence.setText("الثقة التحليلية: --");
                    backtest.setText("📊 الاختبار التاريخي\nغير متاح حاليًا");
                    status.setText("🔴 خطأ تاريخي: " + (error != null ? error : "فشل غير معروف"));
                });
            }
        });
    }

    private void runPrediction(MarketData currentData) {
        if (currentData == null) {
            status.setText("🔴 لا توجد بيانات سعر حالي");
            return;
        }

        if (historicalBars == null || historicalBars.size() < 15) {
            prediction.setText("بيانات تاريخية غير كافية (الحد الأدنى 15 يوم)");
            confidence.setText("الثقة التحليلية: --");
            backtest.setText("📊 الاختبار التاريخي\nغير متاح - البيانات قليلة");
            return;
        }

        try {
            // 1. حساب وتحليل التوقع الرئيسي
            PredictionEngine.PredictionResult result = predictionEngine.analyze(currentData.goldUsd, historicalBars);

            if (result != null) {
                prediction.setText(String.format(Locale.US,
                        "الاتجاه: %s\nالسعر المتوقع: $%.2f\nالبيانات: %d يوم",
                        result.direction, result.predictedPrice, historicalBars.size()));

                confidence.setText(String.format(Locale.US, "الثقة التحليلية: %d%%", result.confidence));
            }

            // 2. تشغيل الـ Backtest بشكل مستقل لتفادي انهيار التطبيق
            try {
                BacktestEngine.BacktestResult testResult = backtestEngine.run(historicalBars);
                if (testResult != null && testResult.totalTests > 0) {
                    StringBuilder display = new StringBuilder();
                    display.append(String.format(Locale.US,
                            "📊 الاختبار التاريخي\n" +
                            "دقة الاتجاه: %.1f%%\n" +
                            "التوقعات الصحيحة: %d من %d\n" +
                            "متوسط خطأ السعر: $%.2f\n\n",
                            testResult.directionAccuracy, testResult.correctTests, testResult.totalTests, testResult.averageAbsoluteError));

                    if (testResult.detailedReport != null) {
                        display.append(testResult.detailedReport);
                    }
                    backtest.setText(display.toString());
                } else {
                    backtest.setText("📊 الاختبار التاريخي\nبيانات غير كافية للاختبار");
                }
            } catch (Exception eBacktest) {
                Log.e(TAG, "Backtest error: ", eBacktest);
                backtest.setText("📊 الاختبار التاريخي\nتعذر الحساب لهذا النطاق");
            }

            String updateTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            status.setText("🟢 تم التحديث بنجاح: " + updateTime);

        } catch (Exception e) {
            Log.e(TAG, "Prediction execution failed: ", e);
            status.setText("🔴 خطأ المعالجة: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataEngine != null) {
            dataEngine.stopAutoUpdate();
        }
    }
}
