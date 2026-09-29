package com.goldai.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

                connection.setUseCaches(false);

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    inputStream =
                            connection.getInputStream();

                } else {

                    inputStream =
                            connection.getErrorStream();
                }

                String responseText =
                        readStream(inputStream);

                if (responseCode != 200) {

                    callback.onError(
                            "HTTP "
                                    + responseCode
                                    + "\n"
                                    + responseText
                    );

                    return;
                }

                if (responseText == null ||
                        responseText.trim().isEmpty()) {

                    callback.onError(
                            "السيرفر رجع استجابة فارغة"
                    );

                    return;
                }

                JSONObject root =
                        new JSONObject(
                                responseText
                        );

                if (!root.has("points")) {

                    callback.onError(
                            "لا يوجد points في استجابة XAUS\n"
                                    + responseText
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

                    history.add(
                            new GoldBar(
                                    date,
                                    close,
                                    high,
                                    low,
                                    close
                            )
                    );
                }

                if (history.size() < 20) {

                    callback.onError(
                            "تم الاتصال بنجاح، لكن عدد الأيام = "
                                    + history.size()
                                    + "\n\n"
                                    + "بداية الاستجابة:\n"
                                    + preview(responseText)
                    );

                    return;
                }

                callback.onSuccess(
                        history
                );

            } catch (Exception e) {

                callback.onError(
                        "نوع الخطأ: "
                                + e.getClass()
                                .getSimpleName()
                                + "\n\n"
                                + "الرسالة:\n"
                                + e.getMessage()
                );

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }

    private String readStream(
            InputStream stream)
            throws Exception {

        if (stream == null) {
            return "";
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
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

        return result.toString();
    }

    private String preview(
            String text) {

        if (text == null) {
            return "";
        }

        if (text.length() <= 500) {
            return text;
        }

        return text.substring(
                0,
                500
        );
    }
}
