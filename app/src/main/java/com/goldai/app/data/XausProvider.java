package com.goldai.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class XausProvider implements MarketDataProvider {

    private static final String API_URL =
            "https://api.goldprice.dev/v1/prices?symbol=XAU-USD-SPOT";

    @Override
    public void getLatestData(Callback callback) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(API_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {
                    callback.onError(
                            "خطأ من المصدر: " + responseCode
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
                        new JSONObject(result.toString());

                JSONArray symbols =
                        root.getJSONArray("symbols");

                JSONObject gold =
                        symbols.getJSONObject(0);

                double goldUsd =
                        gold.getDouble("price");

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
                        "فشل الاتصال: " + e.getMessage()
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}
