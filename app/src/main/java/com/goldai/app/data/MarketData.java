package com.goldai.app.data;

public class MarketData {

    public double goldUsd;
    public double usdEgp;
    public double gold24;
    public double gold21;
    public double gold18;

    public long timestamp;

    public MarketData(
            double goldUsd,
            double usdEgp,
            double gold24,
            double gold21,
            double gold18,
            long timestamp) {

        this.goldUsd = goldUsd;
        this.usdEgp = usdEgp;
        this.gold24 = gold24;
        this.gold21 = gold21;
        this.gold18 = gold18;
        this.timestamp = timestamp;
    }
}
