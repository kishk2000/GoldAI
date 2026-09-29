package com.goldai.app.data;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class XausProvider implements MarketDataProvider {

    private static final String API_URL =
            "https://api.gold-api.com/price/XAU";

    @Override
    public void getLatestData(Callback callback) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(API_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(10000);

                connection.setReadTimeout(10000);

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    callback.onError(
                            "خطأ من مصدر الذهب: "
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

                double goldUsd =
                        root.getDouble("price");

                MarketData data =
                        new MarketData(
                                goldUsd,
                                0,
                                0,
                                0,
                                0,
                                System.currentTimeMillis()
                        );

                callback.onSuccess(data);

            } catch (Exception e) {

                callback.onError(
                        "فشل الاتصال بالذهب: "
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
