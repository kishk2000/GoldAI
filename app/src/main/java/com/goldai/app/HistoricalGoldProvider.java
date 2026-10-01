package com.goldai.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoricalGoldProvider {

    private static final String API_URL = "https://standardbullion.com/api/v1/market/history?metal=XAU&range=3m";

    public interface Callback {
        void onSuccess(List<GoldBar> bars);
        void onError(String error);
    }

    public static class GoldBar {
        public String date;
        public double open;
        public double high;
        public double low;
        public double close;

        public GoldBar(String date, double open, double high, double low, double close) {
            this.date = date != null ? date : "";
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
        }
    }

    public void getLatestHistory(Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    callback.onError("خطأ في مصدر التاريخ: " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(result.toString());
                if (!root.has("points")) {
                    callback.onError("استجابة غير صالحة من خادم التاريخ");
                    return;
                }

                JSONArray points = root.getJSONArray("points");
                List<GoldBar> history = new ArrayList<>();

                for (int i = 0; i < points.length(); i++) {
                    JSONObject point = points.getJSONObject(i);
                    
                    String timestamp = point.optString("t", "");
                    // تنظيف السلسلة واقتطاع الجزء الخاص بالتاريخ فقط YYYY-MM-DD إذا كانت تحتوي على وقت
                    if (timestamp.contains("T")) {
                        timestamp = timestamp.split("T")[0];
                    }

                    double price = point.optDouble("price", 0.0);

                    if (price > 0) {
                        history.add(new GoldBar(timestamp, price, price, price, price));
                    }
                }

                if (history.isEmpty()) {
                    callback.onError("لا توجد بيانات تاريخية متاحة");
                    return;
                }

                // ترتيب البيانات من الأقدم إلى الأحدث
                Collections.sort(history, (a, b) -> a.date.compareTo(b.date));

                // الاحتفاظ بآخر 90 يوم
                if (history.size() > 90) {
                    history = new ArrayList<>(history.subList(history.size() - 90, history.size()));
                }

                callback.onSuccess(history);

            } catch (Exception e) {
                callback.onError("فشل تحميل التاريخ: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}
