package com.goldai.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.goldai.app.data.DataEngine;
import com.goldai.app.data.DemoProvider;
import com.goldai.app.data.MarketData;

public class MainActivity extends Activity {

    int gold = Color.rgb(212, 175, 55);
    int dark = Color.rgb(15, 23, 42);
    int card = Color.rgb(30, 41, 59);
    int white = Color.WHITE;
    int green = Color.rgb(34, 197, 94);

    TextView price;
    TextView change;
    TextView prediction;
    TextView confidence;
    TextView gold24;
    TextView gold21;
    TextView gold18;
    TextView status;

    DataEngine dataEngine;

    Handler handler = new Handler(Looper.getMainLooper());

    Runnable updateTask = new Runnable() {
        @Override
        public void run() {

            dataEngine.update(new com.goldai.app.data.MarketDataProvider.Callback() {

                @Override
                public void onSuccess(MarketData data) {

                    runOnUiThread(() -> {

                        price.setText(
                                String.format("$%.2f", data.goldUsd)
                        );

                        gold24.setText(
                                String.format("عيار 24     %.0f جنيه", data.gold24)
                        );

                        gold21.setText(
                                String.format("عيار 21     %.0f جنيه", data.gold21)
                        );

                        gold18.setText(
                                String.format("عيار 18     %.0f جنيه", data.gold18)
                        );

                        prediction.setText(
                                String.format(
                                        "التوقع القادم: $%.0f",
                                        data.goldUsd
                                )
                        );

                        confidence.setText("الثقة: تجريبية");

                        status.setText(
                                "🟢 البيانات التجريبية تتحدث الآن"
                        );
                    });
                }

                @Override
                public void onError(String error) {

                    runOnUiThread(() ->
                            status.setText("🔴 خطأ: " + error)
                    );
                }
            });

            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataEngine = new DataEngine(
                new DemoProvider()
        );

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(25, 35, 25, 30);
        main.setBackgroundColor(dark);

        TextView title = text("GOLD AI", 32, gold);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle =
                text("تحليل وتوقع أسعار الذهب", 17, white);

        subtitle.setGravity(Gravity.CENTER);

        main.addView(title);
        main.addView(subtitle);

        main.addView(space(25));

        LinearLayout priceCard = card();

        TextView priceTitle =
                text("الذهب العالمي XAU/USD", 18, white);

        price = text("$4,350.00", 34, gold);
        price.setTypeface(Typeface.DEFAULT_BOLD);

        change = text("↗ تحديث تلقائي", 18, green);

        priceCard.addView(priceTitle);
        priceCard.addView(price);
        priceCard.addView(change);

        main.addView(priceCard);

        main.addView(space(15));

        LinearLayout predictionCard = card();

        TextView predictionTitle =
                text("🤖 التوقع اللحظي", 21, gold);

        TextView direction =
                text("↗ محرك التحليل يعمل", 25, green);

        prediction =
                text("التوقع القادم: $4,350", 20, white);

        confidence =
                text("الثقة: تجريبية", 18, white);

        predictionCard.addView(predictionTitle);
        predictionCard.addView(direction);
        predictionCard.addView(prediction);
        predictionCard.addView(confidence);

        main.addView(predictionCard);

        main.addView(space(15));

        LinearLayout localCard = card();

        localCard.addView(
                text("🇪🇬 السوق المصري", 21, gold)
        );

        gold24 =
                text("عيار 24     ---- جنيه", 19, white);

        gold21 =
                text("عيار 21     ---- جنيه", 19, white);

        gold18 =
                text("عيار 18     ---- جنيه", 19, white);

        localCard.addView(gold24);
        localCard.addView(gold21);
        localCard.addView(gold18);

        main.addView(localCard);

        main.addView(space(15));

        status =
                text("🟡 بدء محرك البيانات...", 16, white);

        status.setGravity(Gravity.CENTER);

        main.addView(status);

        setContentView(main);

        handler.post(updateTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(updateTask);
    }

    private LinearLayout card() {

        LinearLayout layout = new LinearLayout(this);

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

        TextView view = new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setPadding(
                0, 5, 0, 5
        );

        return view;
    }

    private TextView space(int height) {

        TextView view = new TextView(this);

        view.setHeight(height);

        return view;
    }
}
