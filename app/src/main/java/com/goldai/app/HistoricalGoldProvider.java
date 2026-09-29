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

    private static final String API_URL =
            "https://standardbullion.com/api/v1/market/history?metal=XAU&range=3m";

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

        public GoldBar(
                String date,
                double open,
                double high,
                double low,
                double close) {

            this.date = date;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
        }
    }

    public void getLatestHistory(
            Callback callback) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(API_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(15000);

                connection.setReadTimeout(15000);

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    callback.onError(
                            "خطأ في مصدر التاريخ: "
                                    + responseCode
                    );

                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {

                    result.append(line);
                }

                reader.close();

                JSONObject root =
                        new JSONObject(
                                result.toString()
                        );

                JSONArray points =
                        root.getJSONArray(
                                "points"
                        );

                List<GoldBar> history =
                        new ArrayList<>();

                for (int i = 0;
                     i < points.length();
                     i++) {

                    JSONObject point =
                            points.getJSONObject(i);

                    String timestamp =
                            point.getString("t");

                    double price =
                            point.getDouble("price");

                    /*
                     * المصدر يعطينا سعر الإغلاق اليومي
                     * وليس OHLC كامل.
                     *
                     * لذلك نستخدم السعر نفسه
                     * كـ Open / High / Low / Close.
                     */

                    history.add(
                            new GoldBar(
                                    timestamp,
                                    price,
                                    price,
                                    price,
                                    price
                            )
                    );
                }

                /*
                 * ترتيب البيانات:
                 * الأقدم ← الأحدث
                 */

                Collections.sort(
                        history,
                        (a, b) ->
                                a.date.compareTo(b.date)
                );

                /*
                 * نحتفظ بآخر 90 يوم
                 * إذا أعاد المصدر أكثر من ذلك.
                 */

                if (history.size() > 90) {

                    history =
                            new ArrayList<>(
                                    history.subList(
                                            history.size() - 90,
                                            history.size()
                                    )
                            );
                }

                callback.onSuccess(
                        history
                );

            } catch (Exception e) {

                callback.onError(
                        "فشل تحميل التاريخ: "
                                + e.getMessage()
                );

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }
}
