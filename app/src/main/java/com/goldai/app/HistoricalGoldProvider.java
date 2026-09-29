package com.goldai.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class HistoricalGoldProvider {

    private static final String API_URL =
            "https://api.goldprice.dev/v1/bars";

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

                LocalDate today =
                        LocalDate.now(
                                ZoneOffset.UTC
                        );

                LocalDate from =
                        today.minusDays(30);

                String urlString =
                        API_URL
                                + "?symbol=XAU-USD-SPOT"
                                + "&interval=1d"
                                + "&from="
                                + from
                                + "&to="
                                + today
                                + "&limit=30";

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(15000);

                connection.setReadTimeout(15000);

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

                JSONArray bars =
                        root.getJSONArray("bars");

                List<GoldBar> history =
                        new ArrayList<>();

                for (int i = 0;
                     i < bars.length();
                     i++) {

                    JSONObject bar =
                            bars.getJSONObject(i);

                    boolean isClosed =
                            bar.getBoolean(
                                    "is_closed"
                            );

                    if (!isClosed) {
                        continue;
                    }

                    String date =
                            bar.getString(
                                    "bar_start"
                            );

                    double open =
                            bar.getDouble(
                                    "open"
                            );

                    double high =
                            bar.getDouble(
                                    "high"
                            );

                    double low =
                            bar.getDouble(
                                    "low"
                            );

                    double close =
                            bar.getDouble(
                                    "close"
                            );

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
