package com.goldai.app.data;

public class DataEngine {

    private final MarketDataProvider goldProvider;
    private final UsdEgpProvider usdEgpProvider;

    public DataEngine(
            MarketDataProvider goldProvider) {

        this.goldProvider = goldProvider;
        this.usdEgpProvider =
                new UsdEgpProvider();
    }

    public void update(
            MarketDataProvider.Callback callback) {

        goldProvider.getLatestData(
                new MarketDataProvider.Callback() {

                    @Override
                    public void onSuccess(
                            MarketData goldData) {

                        usdEgpProvider.getLatestRate(
                                new UsdEgpProvider.Callback() {

                                    @Override
                                    public void onSuccess(
                                            double usdEgp) {

                                        double goldUsd =
                                                goldData.goldUsd;

                                        // وزن الأوقية التروية
                                        // إلى جرام
                                        double goldPerGramUsd =
                                                goldUsd / 31.1034768;

                                        // سعر جرام 24 بالجنيه
                                        double price24 =
                                                goldPerGramUsd
                                                        * usdEgp;

                                        // عيار 21
                                        double price21 =
                                                price24 * 21.0 / 24.0;

                                        // عيار 18
                                        double price18 =
                                                price24 * 18.0 / 24.0;

                                        MarketData result =
                                                new MarketData(
                                                        goldUsd,
                                                        usdEgp,
                                                        price24,
                                                        price21,
                                                        price18,
                                                        System.currentTimeMillis()
                                                );

                                        callback.onSuccess(
                                                result
                                        );
                                    }

                                    @Override
                                    public void onError(
                                            String error) {

                                        callback.onError(
                                                error
                                        );
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(
                            String error) {

                        callback.onError(
                                error
                        );
                    }
                }
        );
    }
}
