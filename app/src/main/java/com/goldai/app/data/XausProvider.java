package com.goldai.app.data;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class XausProvider implements MarketDataProvider {

    private static final String API_URL =
            "https://xaus.com/api/";

    @Override
    public void getLatestData(Callback callback) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {
                URL url = new URL(API_URL);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    callback.onError(
                            "HTTP Error: " + responseCode
                    );
                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(response.toString());

                /*
                 * سنحدد أسماء الحقول النهائية
                 * بعد التأكد من استجابة المصدر.
                 */

                double goldUsd =
                        json.optDouble("XAUUSD", 0);

                long timestamp =
                        System.currentTimeMillis();

                MarketData data =
                        new MarketData(
                                goldUsd,
                                0,
                                0,
                                0,
                                0,
                                timestamp
                        );

                callback.onSuccess(data);

            } catch (Exception e) {

                callback.onError(
                        e.getMessage() != null
                                ? e.getMessage()
                                : "Unknown error"
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}
