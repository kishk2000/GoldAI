package com.goldai.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("GOLD AI");
        title.setTextColor(Color.rgb(255, 215, 70));
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText("\nتحليل الذهب بالذكاء الاصطناعي\n\nجاري تجهيز محرك البيانات...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(20);
        status.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(status);

        setContentView(layout);
    }
}
