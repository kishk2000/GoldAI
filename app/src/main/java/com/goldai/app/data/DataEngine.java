package com.goldai.app.data;

public class DataEngine {

    private final MarketDataProvider provider;

    public DataEngine(MarketDataProvider provider) {
        this.provider = provider;
    }

    public void update(MarketDataProvider.Callback callback) {
        provider.getLatestData(callback);
    }
}
