package com.goldai.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.goldai.app.data.HistoricalGoldProvider;
import com.goldai.app.data.MarketData;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

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

    private HistoricalGoldProvider historyProvider;
    private PredictionEngine predictionEngine;
    private BacktestEngine backtestEngine;

    private List<HistoricalGoldProvider.GoldBar> historicalBars;
    private boolean historyLoaded = false;
    private MarketData latestMarketData;

    private final BroadcastReceiver marketReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "com.goldai.app.MARKET_UPDATE".equals(intent.getAction())) {
                double goldUsd = intent.getDoubleExtra("goldUsd", 0);
                double usdEgp = intent.getDoubleExtra("usdEgp", 0);
                double g24 = intent.getDoubleExtra("gold24", 0);
                double g21 = intent.getDoubleExtra("gold21", 0);
                double g18 = intent.getDoubleExtra("gold18", 0);

                if (goldUsd > 0) {
                    long currentTime = System.currentTimeMillis();
                    MarketData data = new MarketData(goldUsd, usdEgp, g24, g21, g18, currentTime);
                    latestMarketData = data;
                    updateMarketUI(data);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int layoutId = getResources().getIdentifier("activity_main", "layout", getPackageName());
        if (layoutId != 0) {
            setContentView(layoutId);
        }

        initViews();

        predictionEngine = new PredictionEngine();
        backtestEngine = new BacktestEngine();
        historyProvider = new HistoricalGoldProvider();

        // تسجيل مستقبل تحديثات الخدمة الخلفية
        IntentFilter filter = new IntentFilter("com.goldai.app.MARKET_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(marketReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(marketReceiver, filter);
        }

        // 1. جلب البيانات التاريخية
        loadHistoryOnce();

        // 2. جلب الأسعار المباشرة مباشرة فور التشغيل
        fetchDirectMarketData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(marketReceiver);
        } catch (Exception ignored) {}
    }

    private void initViews() {
        priceUsd = getViewByName("txt_price_usd");
        egpUsd = getViewByName("txt_egp_usd");
        egp24 = getViewByName("txt_egp_24");
        egp21 = getViewByName("txt_egp_21");
        egp18 = getViewByName("txt_egp_18");

        prediction = getViewByName("txt_prediction");
        confidence = getViewByName("txt_confidence");
        backtest = getViewByName("txt_backtest");
        status = getViewByName("txt_status");

        setSafeText(status, "⏳ جاري جلب الأسعار المباشرة...");
    }

    private TextView getViewByName(String name) {
        int resId = getResources().getIdentifier(name, "id", getPackageName());
        if (resId != 0) {
            return findViewById(resId);
        }
        return null;
    }

    private void setSafeText(TextView view, String text) {
        if (view != null) {
            view.setText(text);
        }
    }

    private void fetchDirectMarketData() {
        new Thread(() -> {
            try {
                // محاولة جلب السعر مباشرة من الخادم المخصص
                URL url = new URL("https://goldlive.kishk2000.workers.dev/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(builder.toString());
                    double goldUsd = json.optDouble("goldUsd", json.optDouble("price", 2650.0));
                    double usdEgp = json.optDouble("usdEgp", json.optDouble("usd_egp", 49.5));
                    double g24 = json.optDouble("gold24", 0);
                    double g21 = json.optDouble("gold21", 0);
                    double g18 = json.optDouble("gold18", 0);

                    MarketData data = new MarketData(goldUsd, usdEgp, g24, g21, g18, System.currentTimeMillis());
                    latestMarketData = data;

                    runOnUiThread(() -> updateMarketUI(data));
                } else {
                    useFallbackData("رمز الاستجابة: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "Direct fetch failed", e);
                useFallbackData(e.getMessage());
            }
        }).start();
    }

    private void useFallbackData(String reason) {
        // سعر تقريبي احتياطي في حال التعذر لجعل الواجهة تعمل
        double fallbackGoldUsd = 2650.0;
        double fallbackUsdEgp = 49.50;

        MarketData data = new MarketData(fallbackGoldUsd, fallbackUsdEgp, 0, 0, 0, System.currentTimeMillis());
        latestMarketData = data;

        runOnUiThread(() -> {
            updateMarketUI(data);
            setSafeText(status, "⚠️ تعذر اتصال الـ API (" + reason + ") - تم استخدام آخر سعر تقريبي");
        });
    }

    public void updateMarketUI(MarketData data) {
        if (data == null) return;

        runOnUiThread(() -> {
            setSafeText(priceUsd, String.format(Locale.US, "سعر الأونصة: $%.2f", data.goldUsd));
            setSafeText(egpUsd, String.format(Locale.US, "الدولار: %.2f جنيه", data.usdEgp));

            double g24 = data.gold24 > 0 ? data.gold24 : (data.goldUsd * data.usdEgp / 31.1035);
            double g21 = data.gold21 > 0 ? data.gold21 : (g24 * 0.875);
            double g18 = data.gold18 > 0 ? data.gold18 : (g24 * 0.750);

            setSafeText(egp24, String.format(Locale.US, "عيار 24:  %.0f جنيه", g24));
            setSafeText(egp21, String.format(Locale.US, "عيار 21:  %.0f جنيه", g21));
            setSafeText(egp18, String.format(Locale.US, "عيار 18:  %.0f جنيه", g18));

            if (historyLoaded && historicalBars != null && !historicalBars.isEmpty()) {
                runPrediction(data);
            } else {
                setSafeText(status, "⏳ جاري تحليل الشموع التاريخية...");
            }
        });
    }

    private void loadHistoryOnce() {
        historyProvider.getLatestHistory(new HistoricalGoldProvider.Callback() {
            @Override
            public void onSuccess(List<HistoricalGoldProvider.GoldBar> bars) {
                if (bars == null || bars.isEmpty()) {
                    runOnUiThread(() -> {
                        historyLoaded = false;
                        setSafeText(status, "🟡 القائمة التاريخية فارغة");
                    });
                    return;
                }

                historicalBars = bars;
                historyLoaded = true;

                runOnUiThread(() -> {
                    if (latestMarketData != null) {
                        runPrediction(latestMarketData);
                    } else {
                        setSafeText(status, "🟢 تم تحميل " + historicalBars.size() + " شمعة - جاري جلب السعر...");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    historyLoaded = false;
                    setSafeText(prediction, "تعذر تحميل البيانات التاريخية");
                    setSafeText(confidence, "الثقة التحليلية: --");
                    setSafeText(backtest, "📊 الاختبار التاريخي\nغير متاح حاليًا");
                    setSafeText(status, "🔴 خطأ تاريخي: " + (error != null ? error : "فشل غير معروف"));
                });
            }
        });
    }

    private void runPrediction(MarketData currentData) {
        if (currentData == null) {
            setSafeText(status, "🔴 لا توجد بيانات سعر حالي");
            return;
        }

        if (historicalBars == null || historicalBars.size() < 15) {
            setSafeText(prediction, "بيانات تاريخية غير كافية (الحد الأدنى 15 يوم)");
            setSafeText(confidence, "الثقة التحليلية: --");
            setSafeText(backtest, "📊 الاختبار التاريخي\nغير متاح - البيانات قليلة");
            return;
        }

        try {
            PredictionEngine.PredictionResult result = predictionEngine.analyze(currentData.goldUsd, historicalBars);

            if (result != null) {
                setSafeText(prediction, String.format(Locale.US,
                        "الاتجاه المتوقع: %s\nالسعر المتوقع: $%.2f",
                        result.direction, result.predictedPrice));

                setSafeText(confidence, String.format(Locale.US, "نسبة الثقة: %d%%", result.confidence));
            }

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
                    setSafeText(backtest, display.toString());
                } else {
                    setSafeText(backtest, "📊 الاختبار التاريخي\nبيانات غير كافية للاختبار");
                }
            } catch (Exception eBacktest) {
                Log.e(TAG, "Backtest error: ", eBacktest);
                setSafeText(backtest, "📊 الاختبار التاريخي\nتعذر الحساب لهذا النطاق");
            }

            String updateTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            setSafeText(status, "🟢 تم التحديث بنجاح: " + updateTime);

        } catch (Exception e) {
            Log.e(TAG, "Prediction execution failed: ", e);
            setSafeText(status, "🔴 خطأ المعالجة: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
