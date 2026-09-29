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
import com.goldai.app.data.MarketData;
import com.goldai.app.data.XausProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;

    int gold = Color.rgb(212, 175, 55);
    int dark = Color.rgb(15, 23, 42);
    int card = Color.rgb(30, 41, 59);
    int white = Color.WHITE;
    int green = Color.rgb(34, 197, 94);

    TextView price;
    TextView prediction;
    TextView confidence;
    TextView gold24;
    TextView gold21;
    TextView gold18;
    TextView status;

    DataEngine dataEngine;

    Handler handler =
            new Handler(Looper.getMainLooper());

    Runnable updateTask = new Runnable() {

        @Override
        public void run() {

            dataEngine.update(
                    new com.goldai.app.data.MarketDataProvider.Callback() {

                @Override
                public void onSuccess(MarketData data) {

                    runOnUiThread(() -> {

                        price.setText(
                                String.format(
                                        Locale.US,
                                        "$%.2f",
                                        data.goldUsd
                                )
                        );

                        prediction.setText(
                                String.format(
                                        Locale.US,
                                        "السعر الحالي: $%.2f",
                                        data.goldUsd
                                )
                        );

                        confidence.setText(
                                "الثقة: بيانات سوق حقيقية"
                        );

                        gold24.setText(
                                "عيار 24     -- جنيه"
                        );

                        gold21.setText(
                                "عيار 21     -- جنيه"
                        );

                        gold18.setText(
                                "عيار 18     -- جنيه"
                        );

                        String updateTime =
                                new SimpleDateFormat(
                                        "HH:mm:ss",
                                        Locale.getDefault()
                                ).format(
                                        new Date(data.timestamp)
                                );

                        status.setText(
                                "🟢 آخر تحديث: "
                                        + updateTime
                                        + "\n"
                                        + "تحديث تلقائي كل 30 ثانية"
                        );
                    });
                }

                @Override
                public void onError(String error) {

                    runOnUiThread(() ->
                            status.setText(
                                    "🔴 تعذر تحديث البيانات\n"
                                            + error
                            )
                    );
                }
            });

            handler.postDelayed(
                    this,
                    30000
            );
        }
    };

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

        TextView priceTitle =
                text(
                        "الذهب العالمي XAU/USD",
                        18,
                        white
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

        priceCard.addView(priceTitle);
        priceCard.addView(price);

        main.addView(priceCard);

        main.addView(
                space(15)
        );

        LinearLayout predictionCard =
                card();

        TextView predictionTitle =
                text(
                        "🤖 تحليل السعر",
                        21,
                        gold
                );

        prediction =
                text(
                        "جاري تحميل السعر...",
                        20,
                        white
                );

        confidence =
                text(
                        "الثقة: --",
                        18,
                        white
                );

        predictionCard.addView(
                predictionTitle
        );

        predictionCard.addView(
                prediction
        );

        predictionCard.addView(
                confidence
        );

        main.addView(
                predictionCard
        );

        main.addView(
                space(15
