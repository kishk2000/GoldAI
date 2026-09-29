package com.goldai.app.data;

public class DemoProvider implements MarketDataProvider {

    private double goldPrice = 4350.0;

    @Override
    public void getLatestData(Callback callback) {

        new Thread(() -> {

            double movement = (Math.random() - 0.5) * 4.0;
            goldPrice += movement;

            MarketData data = new MarketData(
                    goldPrice,
                    48.50,
                    goldPrice * 0.0321507 * 48.50,
                    goldPrice * 0.0321507 * 48.50 * 0.875,
                    goldPrice * 0.0321507 * 48.50 * 0.75,
                    System.currentTimeMillis()
            );

            callback.onSuccess(data);

        }).start();
    }
}
