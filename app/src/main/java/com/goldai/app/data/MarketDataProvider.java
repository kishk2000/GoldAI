package com.goldai.app.data;

public interface MarketDataProvider {

    void getLatestData(Callback callback);

    interface Callback {
        void onSuccess(MarketData data);
        void onError(String error);
    }
}
