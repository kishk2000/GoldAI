package com.goldai.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HistoricalGoldProvider {

    private static final String API_URL =
            "https://xaus.com/api/v1/history?range=1y";

    public interface Callback {

        void onSuccess(
                List<GoldBar> bars
        );

        void onError(
                String error
        );
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

                connection.setConnectTimeout(20000);

                connection.setReadTimeout(20000);

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    callback.onError(
                            "خطأ من مصدر التاريخ: "
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

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    result.append(line);
                }

                reader.close();

                JSONObject root =
                        new JSONObject(
                                result.toString()
                        );

                if (!root.has("points")) {

                    callback.onError(
                            "مصدر التاريخ لم يرجع بيانات points"
                    );

                    return;
                }

                JSONArray points =
                        root.getJSONArray("points");

                List<GoldBar> history =
                        new ArrayList<>();

                for (
                        int i = 0;
                        i < points.length();
                        i++
                ) {

                    JSONObject point =
                            points.getJSONObject(i);

                    if (!point.has("d") ||
                            !point.has("c") ||
                            !point.has("h") ||
                            !point.has("l")) {

                        continue;
                    }

                    String date =
                            point.getString("d");

                    double close =
                            point.getDouble("c");

                    double high =
                            point.getDouble("h");

                    double low =
                            point.getDouble("l");

                    double open = close;

                    history.add(
                            new GoldBar(
                                    date,
                                    open,
                                    high,
                                    low,
                                    close
                            )
                    );
                }

                if (history.size() < 20) {

                    callback.onError(
                            "بيانات التاريخ غير كافية: "
                                    + history.size()
                                    + " يوم"
                    );

                    return;
                }

                callback.onSuccess(
                        history
                );

            } catch (Exception e) {

                callback.onError(
                        "فشل تحميل التاريخ: "
                                + e.getClass().getSimpleName()
                                + " - "
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
