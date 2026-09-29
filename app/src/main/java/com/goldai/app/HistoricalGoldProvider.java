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
            "https://xaus.com/api/v1/history";

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

                connection.setRequestMethod(
                        "GET"
                );

                connection.setConnectTimeout(
                        15000
                );

                connection.setReadTimeout(
                        15000
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    callback.onError(
                            "خطأ في البيانات التاريخية: "
                                    + responseCode
                    );

                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection
                                                .getInputStream()
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

                JSONArray points =
                        root.getJSONArray(
                                "points"
                        );

                List<GoldBar> history =
                        new ArrayList<>();

                for (
                        int i = 0;
                        i < points.length();
                        i++
                ) {

                    JSONObject point =
                            points.getJSONObject(i);

                    String date =
                            point.getString(
                                    "d"
                            );

                    double close =
                            point.getDouble(
                                    "c"
                            );

                    double high =
                            point.getDouble(
                                    "h"
                            );

                    double low =
                            point.getDouble(
                                    "l"
                            );

                    /*
                     * XAUS history endpoint
                     * يعرض close/high/low.
                     *
                     * لا يوجد open في صيغة النقطة
                     * المستخدمة هنا، لذلك نستخدم close
                     * السابق كقيمة تقريبية للـ open.
                     *
                     * المؤشرات الحالية تعتمد أساسًا
                     * على close، لذلك لن يؤثر ذلك
                     * على RSI وEMA وMomentum.
                     */

                    double open;

                    if (i > 0) {

                        open =
                                points
                                        .getJSONObject(i - 1)
                                        .getDouble("c");

                    } else {

                        open = close;
                    }

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
                            "التاريخ المستلم غير كافٍ: "
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
