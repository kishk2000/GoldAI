package com.goldai.app.data;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UsdEgpProvider {

    private static final String API_URL =
            "https://open.er-api.com/v6/latest/USD";

    public interface Callback {
        void onSuccess(double usdEgp);
        void onError(String error);
    }

    public void getLatestRate(Callback callback) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(API_URL);

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
                            "خطأ من مصدر الدولار: "
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

                JSONObject rates =
                        root.getJSONObject("rates");

                double usdEgp =
                        rates.getDouble("EGP");

                callback.onSuccess(
                        usdEgp
                );

            } catch (Exception e) {

                callback.onError(
                        "فشل الاتصال بالدولار: "
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
