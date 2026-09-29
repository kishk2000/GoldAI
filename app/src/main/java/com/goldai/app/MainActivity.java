package com.goldai.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.goldai.app.data.DataEngine;
import com.goldai.app.data.HistoricalGoldProvider;
import com.goldai.app.data.MarketData;
import com.goldai.app.data.XausProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;

    private final int gold = Color.rgb(212, 175, 55);
    private final int dark = Color.rgb(15, 23, 42);
    private final int card = Color.rgb(30, 41, 59);
    private final int white = Color.WHITE;

    TextView price;
    TextView dollar;
    TextView gold24;
    TextView gold21;
    TextView gold18;

    TextView prediction;
    TextView confidence;
    TextView backtest;
    TextView status;

    DataEngine dataEngine;

    PredictionEngine predictionEngine =
            new PredictionEngine();

    HistoricalGoldProvider historyProvider =
            new HistoricalGoldProvider();

    BacktestEngine backtestEngine =
            new BacktestEngine();

    Handler handler =
            new Handler(Looper.getMainLooper());

    List<HistoricalGoldProvider.GoldBar> historicalBars;

    boolean historyLoaded = false;

    Runnable updateTask = new Runnable() {

        @Override
        public void run() {

            loadMarketData();

            handler.postDelayed(
                    this,
                    30000
            );
        }
    };

    private void loadMarketData() {

        dataEngine.update(
                new com.goldai.app.data.MarketDataProvider.Callback() {

            @Override
            public void onSuccess(
                    MarketData data) {

                runOnUiThread(() -> {

                    price.setText(
                            String.format(
                                    Locale.US,
                                    "$%.2f",
                                    data.goldUsd
                            )
                    );

                    dollar.setText(
                            String.format(
                                    Locale.US,
                                    "الدولار: %.3f جنيه",
                                    data.usdEgp
                            )
                    );

                    gold24.setText(
                            String.format(
                                    Locale.US,
                                    "عيار 24     %.0f جنيه",
                                    data.gold24
                            )
                    );

                    gold21.setText(
                            String.format(
                                    Locale.US,
                                    "عيار 21     %.0f جنيه",
                                    data.gold21
                            )
                    );

                    gold18.setText(
                            String.format(
                                    Locale.US,
                                    "عيار 18     %.0f جنيه",
                                    data.gold18
                            )
                    );

                    if (historyLoaded) {

                        runPrediction(
                                data
                        );

                    } else {

                        status.setText(
                                "🟢 السعر الحالي وصل\n"
                                        + "جاري تحميل البيانات التاريخية..."
                        );
                    }
                });
            }

            @Override
            public void onError(
                    String error) {

                runOnUiThread(() ->
                        status.setText(
                                "🔴 تعذر تحديث السعر\n"
                                        + error
                        )
                );
            }
        });
    }

    private void loadHistoryOnce() {

        historyProvider.getLatestHistory(
                new HistoricalGoldProvider.Callback() {

            @Override
            public void onSuccess(
                    List<HistoricalGoldProvider.GoldBar> bars) {

                historicalBars = bars;
                historyLoaded = true;

                runOnUiThread(() -> {

                    status.setText(
                            "🟢 التاريخ جاهز\n"
                                    + "في انتظار السعر الحالي..."
                    );

                    loadMarketData();
                });
            }

            @Override
            public void onError(
                    String error) {

                runOnUiThread(() -> {

                    historyLoaded = false;

                    prediction.setText(
                            "تعذر تحميل البيانات التاريخية"
                    );

                    confidence.setText(
                            "الثقة التحليلية: --"
                    );

                    backtest.setText(
                            "📊 الاختبار التاريخي\n"
                                    + "غير متاح حاليًا"
                    );

                    status.setText(
                            "🟡 السعر يعمل\n"
                                    + "تعذر تحميل التاريخ"
                    );
                });
            }
        });
    }

    private void runPrediction(
            MarketData currentData) {

        if (historicalBars == null ||
                historicalBars.size() < 5) {

            prediction.setText(
                    "بيانات تاريخية غير كافية"
            );

            confidence.setText(
                    "الثقة التحليلية: --"
            );

            backtest.setText(
                    "📊 الاختبار التاريخي\n"
                            + "بيانات غير كافية"
            );

            return;
        }

        PredictionEngine.PredictionResult result =
                predictionEngine.analyze(
                        currentData.goldUsd,
                        historicalBars
                );

        BacktestEngine.BacktestResult testResult =
                backtestEngine.run(
                        historicalBars
                );

        prediction.setText(
                String.format(
                        Locale.US,
                        "الاتجاه: %s\n"
                                + "السعر المتوقع: $%.2f\n"
                                + "البيانات المستخدمة: %d يوم",
                        result.direction,
                        result.predictedPrice,
                        historicalBars.size()
                )
        );

        confidence.setText(
                String.format(
                        Locale.US,
                        "الثقة التحليلية: %.0f%%",
                        result.confidence
                )
        );

        if (testResult.totalTests > 0) {

            backtest.setText(
                    String.format(
                            Locale.US,
                            "📊 الاختبار التاريخي\n"
                                    + "دقة الاتجاه: %.1f%%\n"
                                    + "التوقعات الصحيحة: %d من %d\n"
                                    + "متوسط خطأ السعر: $%.2f",
                            testResult.directionAccuracy,
                            testResult.correctTests,
                            testResult.totalTests,
                            testResult.averageAbsoluteError
                    )
            );

        } else {

            backtest.setText(
                    "📊 الاختبار التاريخي\n"
                            + "بيانات غير كافية للاختبار"
            );
        }

        String updateTime =
                new SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        new Date(
                                currentData.timestamp
                        )
                );

        status.setText(
                "🟢 آخر تحديث: "
                        + updateTime
                        + "\n"
                        + "السعر يتحدث كل 30 ثانية"
        );
    }

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        dataEngine =
                new DataEngine(
                        new XausProvider()
                );

        LinearLayout main =
                new LinearLayout(this);

        main.setOrientation(
                LinearLayout.VERTICAL
        );

        main.setPadding(
                25, 35, 25, 30
        );

        main.setBackgroundColor(dark);

        TextView title =
                text(
                        "GOLD AI",
                        32,
                        gold
                );

        title.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        TextView subtitle =
                text(
                        "تحليل وتوقع أسعار الذهب",
                        17,
                        white
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        main.addView(title);
        main.addView(subtitle);

        main.addView(
                space(25)
        );

        LinearLayout priceCard =
                card();

        priceCard.addView(
                text(
                        "الذهب العالمي XAU/USD",
                        18,
                        white
                )
        );

        price =
                text(
                        "$----.--",
                        34,
                        gold
                );

        price.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        priceCard.addView(price);

        main.addView(priceCard);

        main.addView(
                space(15)
        );

        LinearLayout predictionCard =
                card();

        predictionCard.addView(
                text(
                        "🤖 تحليل الذهب",
                        21,
                        gold
                )
        );

        prediction =
                text(
                        "جاري تحليل البيانات...",
                        19,
                        white
                );

        confidence =
                text(
                        "الثقة التحليلية: --",
                        18,
                        white
                );

        predictionCard.addView(prediction);
        predictionCard.addView(confidence);

        main.addView(predictionCard);

        main.addView(
                space(15)
        );

        LinearLayout backtestCard =
                card();

        backtestCard.addView(
                text(
                        "📊 اختبار المحرك",
                        21,
                        gold
                )
        );

        backtest =
                text(
                        "جاري إجراء الاختبار التاريخي...",
                        18,
                        white
                );

        backtestCard.addView(backtest);

        main.addView(backtestCard);

        main.addView(
                space(15)
        );

        LinearLayout localCard =
                card();

        localCard.addView(
                text(
                        "🇪🇬 السوق المصري",
                        21,
                        gold
                )
        );

        dollar =
                text(
                        "الدولار: -- جنيه",
                        19,
                        white
                );

        gold24 =
                text(
                        "عيار 24     -- جنيه",
                        19,
                        white
                );

        gold21 =
                text(
                        "عيار 21     -- جنيه",
                        19,
                        white
                );

        gold18 =
                text(
                        "عيار 18     -- جنيه",
                        19,
                        white
                );

        localCard.addView(dollar);
        localCard.addView(gold24);
        localCard.addView(gold21);
        localCard.addView(gold18);

        main.addView(localCard);

        main.addView(
                space(15)
        );

        status =
                text(
                        "🟡 جاري الاتصال بمصادر البيانات...",
                        16,
                        white
                );

        status.setGravity(
                Gravity.CENTER
        );

        main.addView(status);

        setContentView(main);

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_REQUEST
                );

            } else {

                startMarketUpdateService();
            }

        } else {

            startMarketUpdateService();
        }

        /*
         * تحميل التاريخ مرة واحدة فقط.
         */
        loadHistoryOnce();

        /*
         * تحديث السعر كل 30 ثانية.
         */
        handler.post(updateTask);
    }

    private void startMarketUpdateService() {

        Intent serviceIntent =
                new Intent(
                        this,
                        MarketUpdateService.class
                );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(
                    serviceIntent
            );

        } else {

            startService(
                    serviceIntent
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                NOTIFICATION_PERMISSION_REQUEST) {

            startMarketUpdateService();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        handler.removeCallbacks(
                updateTask
        );
    }

    private LinearLayout card() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                25, 22, 25, 22
        );

        layout.setBackgroundColor(card);

        return layout;
    }

    private TextView text(
            String value,
            float size,
            int color) {

        TextView view =
                new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setPadding(
                0, 5, 0, 5
        );

        return view;
    }

    private TextView space(
            int height) {

        TextView view =
                new TextView(this);

        view.setHeight(height);

        return view;
    }
}
